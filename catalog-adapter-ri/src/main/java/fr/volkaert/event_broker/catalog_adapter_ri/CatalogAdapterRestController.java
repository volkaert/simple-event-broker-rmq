package fr.volkaert.event_broker.catalog_adapter_ri;

import fr.volkaert.event_broker.error.BrokerExceptionResponse;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/catalog")
public class CatalogAdapterRestController {

    @Autowired
    CatalogAdapterService service;

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogAdapterRestController.class);

    @GetMapping(value="/event-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EventType>> getEventTypes() {
        LOGGER.info("GET /catalog/event-types called");
        try {
            return ResponseEntity.ok(service.getEventTypes());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/event-types/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventType> getEventTypeByCode(@PathVariable String code) {
        LOGGER.info("GET /catalog/event-types/{} called", code);
        try {
            return ResponseEntity.ok(service.getEventType(code));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/publications", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Publication>> getPublications() {
        LOGGER.info("GET /catalog/publications called");
        try {
            return ResponseEntity.ok(service.getPublications());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/publications/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Publication> getPublicationByCode(@PathVariable String code) {
        LOGGER.info("GET /catalog/publications/{} called", code);
        try {
            return ResponseEntity.ok(service.getPublication(code));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Subscription>> getSubscriptions() {
        LOGGER.info("GET /catalog/subscriptions called");
        try {
            return ResponseEntity.ok(service.getSubscriptions());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/subscriptions/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Subscription> getSubscriptionByCode(@PathVariable String code) {
        LOGGER.info("GET /catalog/subscriptions/{} called", code);
        try {
            return ResponseEntity.ok(service.getSubscription(code));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
