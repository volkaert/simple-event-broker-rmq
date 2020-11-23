package fr.volkaert.event_broker.publication_manager;

import fr.volkaert.event_broker.catalog_client.CatalogClient;
import fr.volkaert.event_broker.error.BrokerException;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.InflightEvent;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.telemetry.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PublicationManagerService {

    @Autowired
    BrokerConfig config;

    @Autowired
    CatalogClient catalog;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    TelemetryService telemetryService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationManagerService.class);

    public InflightEvent publish(InflightEvent inflightEvent) {
        Instant publicationStart = Instant.now();

        telemetryService.eventPublicationRequested(inflightEvent);

        setTimeToLiveInSecondsIfMissingOrInvalid(inflightEvent);

        inflightEvent.setId(UUID.randomUUID().toString());
        inflightEvent.setCreationDate(publicationStart);
        inflightEvent.setExpirationDate(publicationStart.plusSeconds(inflightEvent.getTimeToLiveInSeconds()));

        boolean shouldContinue = checkConditionsForEventPublicationAreMetOrReject(inflightEvent);
        if (! shouldContinue) {
            return inflightEvent; // *** PAY ATTENTION, THERE IS A RETURN HERE !!! ***
        }

        telemetryService.eventPublicationAttempted(inflightEvent);
        try {
            String eventTypeCode = inflightEvent.getEventTypeCode();    // filled in checkConditionsForEventPublicationAreMetOrReject
            String topicExchangeName = "X_" + eventTypeCode;
            rabbitTemplate.convertAndSend(topicExchangeName, null, inflightEvent, message -> {
                message.getMessageProperties().setExpiration(Long.toString(inflightEvent.getTimeToLiveInSeconds() * 1000));
                return message;
            });

            // Another way to send Message as JSON object:
            /*
            String orderJson = objectMapper.writeValueAsString(order);
            Message message = MessageBuilder
                    .withBody(orderJson.getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            this.rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_ORDERS, message);
            */

            telemetryService.eventPublicationSucceeded(inflightEvent, publicationStart);
        } catch (Exception ex) {
            String msg = telemetryService.eventPublicationFailed(inflightEvent, ex, publicationStart);
            throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg, ex);
        }

        LOGGER.debug("Returning the event {}", inflightEvent);
        return inflightEvent;
    }

    private boolean checkConditionsForEventPublicationAreMetOrReject(InflightEvent inflightEvent) {
        String publicationCode = inflightEvent.getPublicationCode();
        if (publicationCode == null || publicationCode.trim().equals("")) {
            String msg = telemetryService.eventPublicationRejectedDueToMissingPublicationCode(inflightEvent);
            throw new BrokerException(HttpStatus.BAD_REQUEST, msg);
        }

        Publication publication = catalog.getPublication(publicationCode);
        if (publication == null) {
            String msg = telemetryService.eventPublicationRejectedDueToInvalidPublicationCode(inflightEvent);
            throw new BrokerException(HttpStatus.BAD_REQUEST, msg);
        }

        if (! publication.isActive()) {
            String msg = telemetryService.eventPublicationRejectedDueToInactivePublication(inflightEvent);
            throw new BrokerException(HttpStatus.BAD_REQUEST, msg);
        }

        String eventTypeCode = publication.getEventTypeCode();
        inflightEvent.setEventTypeCode(eventTypeCode);  // *** CAUTION ***: side effect here !

        EventType eventType = catalog.getEventType(eventTypeCode);
        if (eventType == null) {
            String msg = telemetryService.eventPublicationRejectedDueToInvalidEventTypeCode(inflightEvent);
            throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg);   // It's an internal error, not a client error / bad request (the client does not provide the event type code) !
        }

        return true; // true means the caller should continue its code flow
    }

    private void setTimeToLiveInSecondsIfMissingOrInvalid(InflightEvent eventFromPublisher) {
        if (eventFromPublisher.getTimeToLiveInSeconds() == null) {
            eventFromPublisher.setTimeToLiveInSeconds(config.getDefaultTimeToLiveInSeconds());
        }
        if (eventFromPublisher.getTimeToLiveInSeconds() < 0) {
            eventFromPublisher.setTimeToLiveInSeconds(config.getDefaultTimeToLiveInSeconds());
        }
        if (eventFromPublisher.getTimeToLiveInSeconds() > config.getMaxTimeToLiveInSeconds()) {
            eventFromPublisher.setTimeToLiveInSeconds(config.getMaxTimeToLiveInSeconds());
        }
    }
}
