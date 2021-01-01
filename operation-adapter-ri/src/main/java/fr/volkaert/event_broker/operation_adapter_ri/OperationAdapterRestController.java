package fr.volkaert.event_broker.operation_adapter_ri;

import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.QueueInfo;
import fr.volkaert.event_broker.error.BrokerExceptionResponse;
import fr.volkaert.event_broker.model.EventToSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class OperationAdapterRestController {

    @Autowired
    OperationAdapterService service;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationAdapterRestController.class);

    @GetMapping(value="/subscriptions/{subscriptionCode}/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventToSubscriber> getNextEventForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("GET /subscriptions/{}/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.getNextEventForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventToSubscriber> deleteNextEventForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.deleteNextEventForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAllEventsForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/events called", subscriptionCode);
        try {
            service.deleteAllEventsForSubscription(subscriptionCode);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/subscriptions/{subscriptionCode}/dead-letter-queue/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventToSubscriber> getNextEventInDeadLetterQueueForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("GET /subscriptions/{}/dead-letter-queue/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.getNextEventInDeadlLetterQueueForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/dead-letter-queue/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventToSubscriber> deleteNextEventInDeadLetterQueueForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/dead-letter-queue/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.deleteNextEventInDeadlLetterQueueForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/dead-letter-queue/events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAllEventsInDeadLetterQueueForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/dead-letter-queue/events called", subscriptionCode);
        try {
            service.deleteAllEventsInDeadlLetterQueueForSubscription(subscriptionCode);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/subscriptions/{subscriptionCode}/rabbitmq/queue/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QueueInfo> getRabbitMQQueueInfoForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("GET /subscriptions/{}/rabbitmq/queue/info called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.getRabbitMQQueueInfoForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/rabbitmq/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OverviewResponse> getRabbitMQOverview() {
        LOGGER.info("GET /rabbitmq/overview called");
        try {
            return ResponseEntity.ok(service.getRabbitMQQueueOverview());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value="/event-processing/activate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> activateEventProcessing() {
        LOGGER.info("POST /event-processing/activate called");
        try {
            return ResponseEntity.ok(service.activateEventProcessing());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value="/event-processing/deactivate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deactivateEventProcessing() {
        LOGGER.info("POST /event-processing/deactivate called");
        try {
            return ResponseEntity.ok(service.deactivateEventProcessing());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
