package fr.volkaert.event_broker.subscription_manager;

import com.rabbitmq.client.Channel;
import fr.volkaert.event_broker.catalog_adapter_client.CatalogAdapterClient;
import fr.volkaert.event_broker.error.BrokerException;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.InflightEvent;
import fr.volkaert.event_broker.model.Subscription;
import fr.volkaert.event_broker.subscription_manager.config.BrokerConfigForSubscriptionManager;
import fr.volkaert.event_broker.telemetry.TelemetryService;
import fr.volkaert.event_broker.util.RabbitMQNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Configuration
@EnableScheduling
public class SubscriptionManagerService {

    @Autowired
    BrokerConfigForSubscriptionManager config;

    @Autowired
    CatalogAdapterClient catalog;

    //@Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    ConnectionFactory rabbitMQConnectionFactory;

    //@Autowired
    //SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;

    @Autowired
    Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @Autowired
    @Qualifier("RestTemplateForSubscriptionAdapter")
    RestTemplate restTemplate;

    Map<String, SimpleMessageListenerContainer> subscriptionCodeToRabbitMQConsumerMap = new ConcurrentHashMap<>();

    @Autowired
    TelemetryService telemetryService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionManagerService.class);

    public void start() {
        LOGGER.info("Subscription service started");
        createRabbitMQConsumers();
    }

    @Scheduled(fixedDelay = 10000)
    // *** NEVER LET AN EXCEPTION BE RAISED/THROWN BY THIS OPERATION !!! ***
    public void createRabbitMQConsumers() {
        try {
            LOGGER.info("Loading subscriptions from the catalog...");
            List<Subscription> subscriptions = catalog.getSubscriptions();
            LOGGER.info("Subscriptions successfully loaded from the catalog...");

            if (subscriptions != null && ! subscriptions.isEmpty()) {
                LOGGER.info("Creating RabbitMQ consumers...");
                for (Subscription subscription : subscriptions) {
                    try {
                        String eventTypeCode = subscription.getEventTypeCode();
                        if (shouldTheEventBeManagedByThisInstanceOfSubscriptionManager(eventTypeCode)) {
                            createRabbitMQConsumerIfAbsent(eventTypeCode, subscription.getCode());
                        }
                    } catch (Exception ex) {  // if there is an issue with a subscription, continue with the others...
                        // No need to log the error since it has already been logged in createRabbitMQConsumer()
                    }
                }
                LOGGER.info("End of RabbitMQ consumers creation");
            }
        }
        catch (Exception ex) {
            LOGGER.error("Error while loading subscriptions from the catalog. RabbitMQ consumers may not have been successfully created.", ex);
        }
    }

    private synchronized SimpleMessageListenerContainer createRabbitMQConsumerIfAbsent(String eventTypeCode, String subscriptionCode) {

        // From https://docs.spring.io/spring-amqp/docs/1.4.5.RELEASE/reference/html/amqp.html: When a listener throws
        // an exception, it is wrapped in a ListenerExecutionFailedException and, normally the message is rejected and
        // requeued by the broker. Setting defaultRequeueRejected to false will cause messages to be discarded (or routed
        // to a dead letter exchange).
        final boolean defaultRequeueRejected = false;

        SimpleMessageListenerContainer rabbitMQListenerContainer = subscriptionCodeToRabbitMQConsumerMap.computeIfAbsent(subscriptionCode, x -> {
            try {
                String exchangeNameForEventType = RabbitMQNames.getExchangeNameForEventType(eventTypeCode);
                String queueNameForSubscription = RabbitMQNames.getQueueNameForSubscription(subscriptionCode);
                String deadLetterExchangeNameForSubscription = RabbitMQNames.getDeadLetterExchangeNameForSubscription(subscriptionCode);
                String deadLetterQueueNameForSubscription = RabbitMQNames.getDeadLetterQueueNameForSubscription(subscriptionCode);

                // NOMINAL **********

                Exchange exchangeForEventType = ExchangeBuilder.fanoutExchange(exchangeNameForEventType).durable(true).build();
                rabbitAdmin.declareExchange(exchangeForEventType);

                Queue queueForSubscription = QueueBuilder
                        .durable(queueNameForSubscription)
                        .deadLetterExchange(deadLetterExchangeNameForSubscription)
                        //.withArgument("x-dead-letter-exchange", deadLetterExchangeNameForSubscription)
                        .deadLetterRoutingKey(deadLetterQueueNameForSubscription)
                        //.withArgument("x-dead-letter-routing-key", deadLetterQueueNameForSubscription)
                        .build();
                rabbitAdmin.declareQueue(queueForSubscription);

                Binding binding = BindingBuilder.bind(queueForSubscription).to(exchangeForEventType).with(queueNameForSubscription).noargs();
                rabbitAdmin.declareBinding(binding);

                // DEAD LETTER **********

                Exchange deadLetterExchange = ExchangeBuilder.directExchange(deadLetterExchangeNameForSubscription).build();
                rabbitAdmin.declareExchange(deadLetterExchange);

                Queue deadLetterQueue = QueueBuilder
                        .durable(deadLetterQueueNameForSubscription)
                        .ttl((int)config.getTimeToLiveInSecondsForDeadLetterQueues() * 1000)
                        .build();
                rabbitAdmin.declareQueue(deadLetterQueue);

                Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterQueueNameForSubscription).noargs();
                rabbitAdmin.declareBinding(deadLetterBinding);

                // LISTENER **********

                Object rabbitMQListener = (ChannelAwareMessageListener) (message, channel) -> {
                    // Since all errors are handled by handleRabbitMQMessage(), NEVER LET AN EXCEPTION BE RAISED HERE !!!
                    try {
                        InflightEvent inflightEvent = (InflightEvent)jackson2JsonMessageConverter.fromMessage(message);
                        handleRabbitMQMessage(inflightEvent, eventTypeCode, subscriptionCode, message, channel);
                    } catch (Exception ex) {
                        LOGGER.error("Unexpected error while handling RabbitMQ message", ex);
                    }
                };
                MessageListenerAdapter rabbitMQListenerAdapter = new MessageListenerAdapter(rabbitMQListener, jackson2JsonMessageConverter);
                rabbitMQListenerAdapter.setDefaultRequeueRejected(defaultRequeueRejected);

                // LISTENER CONTAINER **********

                SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitMQConnectionFactory);
                container.setForceCloseChannel(true);
                container.setShutdownTimeout(0);
                container.setMessageListener(rabbitMQListenerAdapter);
                container.setQueueNames(queueNameForSubscription);
                container.setDefaultRequeueRejected(defaultRequeueRejected);
                container.setConcurrentConsumers(1);
                container.setMaxConcurrentConsumers(1);
                container.start();

                return container;
            } catch (Exception ex) {
                String msg = String.format("Error while creating a RabbitMQ consumer for eventTypeCode %s and subscriptionCode %s",
                        eventTypeCode, subscriptionCode);
                LOGGER.error(msg, ex);
                return null;
            }
        });
        if (rabbitMQListenerContainer == null) {
            // No Need to log the error since it has already been logged in subscriptionCodeToRabbitMQConsumerMap.computeIfAbsent
            String msg = String.format("Error while creating a RabbitMQ consumer for eventTypeCode %s and subscriptionCode %s",
                    eventTypeCode, subscriptionCode);
            throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
        return rabbitMQListenerContainer;
    }


    private void handleRabbitMQMessage(InflightEvent inflightEvent, String eventTypeCode, String subscriptionCode, Message rabbitMQMessage, Channel rabbitMQChannel) {
        Instant deliveryStart = Instant.now();

        try {
            inflightEvent.setSubscriptionCode(subscriptionCode);

            Map<String, Object> headers = rabbitMQMessage.getMessageProperties().getHeaders();
            Integer retriesHeader = (Integer)headers.get("x-retries");
            if (retriesHeader == null) {
                retriesHeader = Integer.valueOf(0);
            }
            boolean redelivered = retriesHeader > 0;
            int redeliveryCount = retriesHeader;
            inflightEvent.setRedelivered(redelivered);
            inflightEvent.setRedeliveryCount(redeliveryCount);

            telemetryService.eventDeliveryRequested(inflightEvent);

            LOGGER.debug("Event received from RabbitMQ. Event is {}.", inflightEvent.cloneWithoutSensitiveData());

            boolean shouldContinue = checkConditionsForEventDeliveryAreMetOrAbort(inflightEvent, deliveryStart, rabbitMQMessage, rabbitMQChannel);
            if (! shouldContinue) {
                // since we return here without raising any exception, the message will be automatically acknowledged
                return; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
            }

            Subscription subscription = catalog.getSubscription(subscriptionCode);

            inflightEvent.setWebhookUrl(subscription.getWebhookUrl());
            inflightEvent.setWebhookContentType(subscription.getWebhookContentType());
            inflightEvent.setWebhookHeaders(subscription.getWebhookHeaders());
            inflightEvent.setAuthMethod(subscription.getAuthMethod());
            inflightEvent.setAuthClientId(subscription.getAuthClientId());
            inflightEvent.setAuthClientSecret(subscription.getAuthClientSecret());
            inflightEvent.setAuthScope(subscription.getAuthScope());
            inflightEvent.setSecret(subscription.getSecret());

            telemetryService.eventDeliveryAttempted(inflightEvent);
            try {
                inflightEvent = callSubscriptionAdapter(inflightEvent);
            } catch (Exception ex) {
                telemetryService.eventDeliveryFailed(inflightEvent, ex, deliveryStart);
                LOGGER.warn("RabbitMQ consumer will be destroyed because an exception was raised while calling the Subscription Adapter. Event is {}.",
                        inflightEvent.toShortLog());
                destroyRabbitMQConsumerForTheSubscription(subscriptionCode);
                return; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
            }

            if (inflightEvent.isWebhookConnectionErrorOccurred() ||
                    inflightEvent.isWebhookReadTimeoutErrorOccurred() ||
                    inflightEvent.isWebhookServer5xxErrorOccurred() ||
                    inflightEvent.isWebhookClient4xxErrorOccurred() ||
                    inflightEvent.isWebhookRedirection3xxErrorOccurred()) {
                handleWebhookErrorOccurred(inflightEvent, deliveryStart, subscription, rabbitMQMessage, rabbitMQChannel);
                return; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
            }

            if (! (inflightEvent.getWebhookHttpStatus() >= 200 && inflightEvent.getWebhookHttpStatus() < 300)) {
                telemetryService.eventDeliveryFailed(inflightEvent, null, deliveryStart);
                LOGGER.warn("RabbitMQ consumer will be destroyed because the webhook returned the unsuccessful http status {}. Event is {}.",
                        inflightEvent.getWebhookHttpStatus(), inflightEvent.toShortLog());
                destroyRabbitMQConsumerForTheSubscription(subscriptionCode);
                return; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
            }

            // If we reached this line, everything seems fine, so we can ack the message.
            // In fact, there is nothing to do to ack an event; since no exception has been raised so far,
            // the event will be automatically acknowledged !
            LOGGER.debug("Event handled successfully. Event is {}.", inflightEvent.toShortLog());
            telemetryService.eventDeliverySucceeded(inflightEvent, deliveryStart);

        } catch (Exception ex) {
            LOGGER.error("Unexpected error while handling RabbitMQ message. Event is {}",
                    (inflightEvent != null ? inflightEvent.toShortLog() : "null"), ex);
            if (inflightEvent != null) {
                telemetryService.eventDeliveryFailed(inflightEvent, null, deliveryStart);
            }
            LOGGER.warn("RabbitMQ consumer will be destroyed because an unexpected error occurred while handling RabbitMQ message. Event is {}.",
                    (inflightEvent != null ? inflightEvent.toShortLog() : "null"));
            destroyRabbitMQConsumerForTheSubscription(subscriptionCode);
        }
    }

    private boolean checkConditionsForEventDeliveryAreMetOrAbort(InflightEvent inflightEvent, Instant deliveryStart,
                                                                 Message rabbitMQMessage, Channel rabbitMQChannel) {

        String subscriptionCode = inflightEvent.getSubscriptionCode();

        if (! config.isEventProcessingActive()) {
            String msg = telemetryService.eventDeliveryAbortedDueToInactiveEventProcessing(inflightEvent);
            LOGGER.warn("Event ignored because the event processing is not active (probably because we are on the backup datacenter, to avoid duplicate deliveries). " +
                    "Event is {}.", inflightEvent.toShortLog());
            return false; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        Subscription subscription = catalog.getSubscription(subscriptionCode);
        if (subscription == null) {
            String msg = telemetryService.eventDeliveryAbortedDueToInvalidSubscriptionCode(inflightEvent);
            LOGGER.warn("Event ignored because the subscription code '{}' is invalid (and thus cannot be sent to a DeadLetterExchange for this subscription). " +
                    "Event is {}.", subscriptionCode, inflightEvent.toShortLog());
            return false; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        if (! subscription.isActive()) {
            telemetryService.eventDeliveryAbortedDueToInactiveSubscription(inflightEvent);
            LOGGER.warn("Event sent to DeadLetterExchange because the subscription '{}' is not active. Event is {}.", subscriptionCode, inflightEvent.toShortLog());
            sendEventInDeadLetterExchange(inflightEvent, rabbitMQMessage, rabbitMQChannel);
            return false; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        String eventTypeCode = subscription.getEventTypeCode();
        EventType eventType = catalog.getEventType(eventTypeCode);
        if (eventType == null) {
            String msg = telemetryService.eventDeliveryAbortedDueToInvalidEventTypeCode(inflightEvent);
            LOGGER.warn("Event sent to DeadLetterExchange because the event type code '{}' is invalid. Event is {}.", eventTypeCode, inflightEvent.toShortLog());
            sendEventInDeadLetterExchange(inflightEvent, rabbitMQMessage, rabbitMQChannel);
            return false; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        if (! eventType.isActive()) {
            telemetryService.eventDeliveryAbortedDueToInactiveEventType(inflightEvent);
            LOGGER.warn("Event sent to DeadLetterExchange because the event type '{}' is not active. Event is {}.", eventTypeCode, inflightEvent.toShortLog());
            sendEventInDeadLetterExchange(inflightEvent, rabbitMQMessage, rabbitMQChannel);
            return false; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        boolean channelOk = subscription.getChannel() == null || subscription.getChannel().equalsIgnoreCase(inflightEvent.getChannel());
        if (! channelOk) {
            telemetryService.eventDeliveryAbortedDueToNotMatchingChannel(inflightEvent);
            LOGGER.warn("Unmatched channel. Event is {}.", inflightEvent.toShortLog());
            // DO NOT sendEventInDeadLetterExchange(inflightEvent) for an unmatched channel !
            return false; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        return true; // true means the caller should continue its code flow
    }

    private void handleWebhookErrorOccurred(InflightEvent inflightEvent,
                                            Instant deliveryStart, Subscription subscription,
                                            Message rabbitMQMessage, Channel rabbitMQChannel) {
        telemetryService.eventDeliveryFailed(inflightEvent, null, deliveryStart);

        boolean eventExpiredDueToTimeToLiveForWebhookError = false;
        String eventExpirationReason = null;

        if (inflightEvent.isWebhookConnectionErrorOccurred()) {
            eventExpiredDueToTimeToLiveForWebhookError = isEventExpiredDueToTimeToLiveForWebhookError(inflightEvent, deliveryStart,
                    config.getDefaultTimeToLiveInSecondsForWebhookConnectionError(),
                    subscription.getTimeToLiveInSecondsForWebhookConnectionError());
            eventExpirationReason = "connection";
        }
        else if (inflightEvent.isWebhookReadTimeoutErrorOccurred()) {
            eventExpiredDueToTimeToLiveForWebhookError = isEventExpiredDueToTimeToLiveForWebhookError(inflightEvent, deliveryStart,
                    config.getDefaultTimeToLiveInSecondsForWebhookReadTimeoutError(),
                    subscription.getTimeToLiveInSecondsForWebhookReadTimeoutError());
            eventExpirationReason = "read timeout";
        }
        else if (inflightEvent.isWebhookServer5xxErrorOccurred()) {
            eventExpiredDueToTimeToLiveForWebhookError = isEventExpiredDueToTimeToLiveForWebhookError(inflightEvent, deliveryStart,
                    config.getDefaultTimeToLiveInSecondsForWebhookServer5xxError(),
                    subscription.getTimeToLiveInSecondsForWebhookServer5xxError());
            eventExpirationReason = "server 5xx";
        }
        else if (inflightEvent.isWebhookClient4xxErrorOccurred()) {
            if (inflightEvent.getWebhookHttpStatus() == HttpStatus.UNAUTHORIZED.value() ||
                    inflightEvent.getWebhookHttpStatus() == HttpStatus.FORBIDDEN.value()) {
                eventExpiredDueToTimeToLiveForWebhookError = isEventExpiredDueToTimeToLiveForWebhookError(inflightEvent, deliveryStart,
                        config.getDefaultTimeToLiveInSecondsForWebhookAuth401Or403Error(),
                        subscription.getTimeToLiveInSecondsForWebhookAuth401Or403Error());
                eventExpirationReason = "auth 401 or 403";
            }
            else {
                eventExpiredDueToTimeToLiveForWebhookError = isEventExpiredDueToTimeToLiveForWebhookError(inflightEvent, deliveryStart,
                        config.getDefaultTimeToLiveInSecondsForWebhookClient4xxError(),
                        subscription.getTimeToLiveInSecondsForWebhookClient4xxError());
                eventExpirationReason = "client 4xx";
            }
        }

        if (eventExpiredDueToTimeToLiveForWebhookError) {
            LOGGER.warn("Event expired before delivery due to time to live expiration because of a webhook {} error. Event is {}.",
                    eventExpirationReason, inflightEvent.toShortLog());
            LOGGER.warn("Event sent to DeadLetterExchange because the event has expired. Event is {}.", inflightEvent.toShortLog());
            sendEventInDeadLetterExchange(inflightEvent, rabbitMQMessage, rabbitMQChannel);
        }
        else {
            LOGGER.warn("RabbitMQ consumer will be destroyed because the webhook returned the unsuccessful http status {}. Event is {}.",
                    inflightEvent.getWebhookHttpStatus(), inflightEvent.toShortLog());
            destroyRabbitMQConsumerForTheSubscription(subscription.getCode());
        }
    }

    // This operation can throw a BrokerException
    private InflightEvent callSubscriptionAdapter(InflightEvent inflightEvent) {
        String subscriptionAdapterUrl = config.getSubscriptionAdapterUrl() + "/webhooks";

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (!StringUtils.isEmpty(config.getAuthClientIdForSubscriptionAdapter()) && !StringUtils.isEmpty(config.getAuthClientSecretForSubscriptionAdapter())) {
            httpHeaders.setBasicAuth(
                    config.getAuthClientIdForSubscriptionAdapter(),
                    config.getAuthClientSecretForSubscriptionAdapter());
        } else {
            LOGGER.warn("No Basic Auth credentials provided to access the Subscription Adapter");
        }

        // charset UTF8 has been defined during the creation of RestTemplate

        HttpEntity<InflightEvent> request = new HttpEntity<>(inflightEvent, httpHeaders);

        try {

            LOGGER.debug("Calling the Subscription Adapter at {}. Event is {}.",
                    subscriptionAdapterUrl, inflightEvent.cloneWithoutSensitiveData());
            ResponseEntity<InflightEvent> response = restTemplate.exchange(
                    subscriptionAdapterUrl, HttpMethod.POST, request, InflightEvent.class);
            LOGGER.debug("The Subscription Adapter returned the http status code {}. Event is {}.",
                    response.getStatusCode(), inflightEvent.toShortLog());

            InflightEvent returnedInflightEvent = response.getBody();
            LOGGER.debug("Returning the event {}", returnedInflightEvent != null ? returnedInflightEvent.cloneWithoutSensitiveData() : null);
            return returnedInflightEvent;

        } catch (HttpStatusCodeException ex) {
            String msg = String.format("Error %s while calling the Subscription Adapter at %s. Event is %s.",
                    ex.getStatusCode(), subscriptionAdapterUrl, inflightEvent.toShortLog());
            LOGGER.error(msg, ex);
            throw new BrokerException(ex.getStatusCode(), msg, ex, subscriptionAdapterUrl);

        } catch (Exception ex) {
            if (ex.getMessage().contains("Connection refused")) {
                String msg = String.format("Connection Refused error while calling the Subscription Adapter at %s. Event is %s.",
                        subscriptionAdapterUrl, inflightEvent.toShortLog());
                LOGGER.error(msg, ex);
                throw new BrokerException(HttpStatus.BAD_GATEWAY, msg, ex, subscriptionAdapterUrl);
            }

            else if (ex.getMessage().contains("Read timed out")) {
                String msg = String.format("Read Timeout error while calling the Subscription Adapter at %s. Event is %s.",
                        subscriptionAdapterUrl, inflightEvent.toShortLog());
                LOGGER.error(msg, ex);
                throw new BrokerException(HttpStatus.GATEWAY_TIMEOUT, msg, ex, subscriptionAdapterUrl);
            }

            else {
                String msg = String.format("Error while calling the Subscription Adapter at %s. Event is %s.",
                        subscriptionAdapterUrl, inflightEvent.toShortLog());
                LOGGER.error(msg, ex);
                throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg, ex, subscriptionAdapterUrl);
            }
        }
    }

    private void destroyRabbitMQConsumerForTheSubscription(String subscriptionCode) {
        LOGGER.warn("Destroying the RabbitMQConsumer for the subscription {}", subscriptionCode);
        SimpleMessageListenerContainer rabbitMQListenerContainer = subscriptionCodeToRabbitMQConsumerMap.remove(subscriptionCode);
        if (rabbitMQListenerContainer != null) {
            try {
                rabbitMQListenerContainer.destroy();
            } catch (Exception ex) {
                String msg = String.format("Could not destroy the RabbitMQConsumer for the subscription %s", subscriptionCode);
                LOGGER.error(msg, ex);
            }
        }
    }

    private void sendEventInDeadLetterExchange(InflightEvent inflightEvent, Message rabbitMQMessage, Channel rabbitMQChannel) {
        if (inflightEvent == null) return;
        try {
            LOGGER.warn("Sending event in the DeadLetterExchange for eventTypeCode {} and subscriptionCode {}. Event is {}.",
                    inflightEvent.getEventTypeCode(), inflightEvent.getSubscriptionCode(), inflightEvent.toShortLog());
            String nameForDeadLetterExchange = RabbitMQNames.getDeadLetterExchangeNameForSubscription(inflightEvent.getSubscriptionCode());
            rabbitTemplate.convertAndSend(nameForDeadLetterExchange, null, inflightEvent, message -> {
                message.getMessageProperties().setExpiration(Long.toString(config.getTimeToLiveInSecondsForDeadLetterMessages() * 1000));
                return message;
            });
        } catch (Exception ex) {
            LOGGER.error("Error while sending an event in the DeadLetterExchange for eventTypeCode {} and subscriptionCode {}. Event is {}.",
                    inflightEvent.getEventTypeCode(), inflightEvent.getSubscriptionCode(), inflightEvent.toShortLog(), ex);
        }
    }

    private boolean isEventExpiredDueToTimeToLiveForWebhookError(InflightEvent event, Instant now, long defaultTimeToLiveForWebhookError, Long timeToLiveForWebhookErrorInSubscription) {
        long timeToLiveInSecondsToUse = defaultTimeToLiveForWebhookError;
        if (timeToLiveForWebhookErrorInSubscription != null && timeToLiveForWebhookErrorInSubscription > 0) {
            timeToLiveInSecondsToUse = timeToLiveForWebhookErrorInSubscription;
        }
        return now.isAfter(event.getCreationDate().plusSeconds(timeToLiveInSecondsToUse));
    }

    private boolean shouldTheEventBeManagedByThisInstanceOfSubscriptionManager(String eventTypeCode) {
        int indexOfTheInstanceOfTheInstanceOfSubscriptionManagerThatShouldManageThisEvent = eventTypeCode.hashCode() % config.getClusterSize();
        return indexOfTheInstanceOfTheInstanceOfSubscriptionManagerThatShouldManageThisEvent == config.getClusterIndex();
    }

    public void activateEventProcessing() {
        config.setEventProcessingActive(true);
        LOGGER.info("Event processing is now active");
    }
    public void deactivateEventProcessing() {
        config.setEventProcessingActive(false);
        LOGGER.info("Event processing is now NOT active");
    }
}
