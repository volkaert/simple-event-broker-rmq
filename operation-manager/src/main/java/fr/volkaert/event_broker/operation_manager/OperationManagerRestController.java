package fr.volkaert.event_broker.operation_manager;

import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.QueueInfo;
import fr.volkaert.event_broker.error.BrokerExceptionResponse;
import fr.volkaert.event_broker.model.InflightEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class OperationManagerRestController {

    @Autowired
    OperationManagerService service;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationManagerRestController.class);

    @GetMapping(value="/subscriptions/{subscriptionCode}/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InflightEvent> getNextEventForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("GET /subscriptions/{}/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.getNextEventForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InflightEvent> deleteNextEventForSubscription(@PathVariable String subscriptionCode) {
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
    public ResponseEntity<InflightEvent> getNextEventInDeadLetterQueueForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("GET /subscriptions/{}/dead-letter-queue/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.getNextEventInDeadLetterQueueForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/dead-letter-queue/events/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InflightEvent> deleteNextEventInDeadLetterQueueForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/dead-letter-queue/events/next called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.deleteNextEventInDeadLetterQueueForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/dead-letter-queue/events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAllEventsInDeadLetterQueueForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/dead-letter-queue/events called", subscriptionCode);
        try {
            service.deleteAllEventsInDeadLetterQueueForSubscription(subscriptionCode);
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
            return ResponseEntity.ok(service.getRabbitMQOverview());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
