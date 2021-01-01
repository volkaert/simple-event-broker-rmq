package fr.volkaert.event_broker.publication_manager;

import fr.volkaert.event_broker.catalog_adapter_client.CatalogAdapterClient;
import fr.volkaert.event_broker.error.BrokerException;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.InflightEvent;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.publication_manager.config.BrokerConfig;
import fr.volkaert.event_broker.telemetry.TelemetryService;
import fr.volkaert.event_broker.util.RabbitMQNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PublicationManagerService {

    @Autowired
    BrokerConfig config;

    @Autowired
    CatalogAdapterClient catalog;

    @Autowired
    @Qualifier("DefaultRabbitTemplate")
    RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier("RabbitTemplateForMirroring")
    RabbitTemplate rabbitTemplateForMirroring;

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
            String topicExchangeName = RabbitMQNames.getExchangeNameForEventType(eventTypeCode);
            rabbitTemplate.convertAndSend(topicExchangeName, null, inflightEvent, message -> {
                message.getMessageProperties().setExpiration(Long.toString(inflightEvent.getTimeToLiveInSeconds() * 1000));
                return message;
            });

            if (config.isMirroringActive()) {
                publishToMirror(topicExchangeName, inflightEvent);
            }

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

    // Never let an exception be raised because of a failure for mirroring (it must not be on the critical path)
    private void publishToMirror(String topicExchangeName, InflightEvent inflightEvent) {
        try {
            rabbitTemplateForMirroring.convertAndSend(topicExchangeName, null, inflightEvent, message -> {
                message.getMessageProperties().setExpiration(Long.toString(inflightEvent.getTimeToLiveInSeconds() * 1000));
                return message;
            });
        } catch (Exception ex) {
            telemetryService.eventPublicationFailedForMirroring(inflightEvent, ex);
        }
    }
}
