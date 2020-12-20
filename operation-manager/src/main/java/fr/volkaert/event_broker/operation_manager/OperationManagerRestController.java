package fr.volkaert.event_broker.operation_manager;

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

    @GetMapping(value="/subscriptions/{subscriptionCode}/events/last", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InflightEvent> getLastEventForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("GET /subscriptions/{}/events/last called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.getLastEventForSubscription(subscriptionCode));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value="/subscriptions/{subscriptionCode}/events/last", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InflightEvent> deleteLastEventForSubscription(@PathVariable String subscriptionCode) {
        LOGGER.info("DELETE /subscriptions/{}/events/last called", subscriptionCode);
        try {
            return ResponseEntity.ok(service.deleteLastEventForSubscription(subscriptionCode));
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
}
