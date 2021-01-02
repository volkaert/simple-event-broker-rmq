package fr.volkaert.event_broker.publication_adapter_ri;

import fr.volkaert.event_broker.error.BrokerException;
import fr.volkaert.event_broker.error.BrokerExceptionResponse;
import fr.volkaert.event_broker.model.EventFromPublisher;
import fr.volkaert.event_broker.model.EventToPublisher;
import fr.volkaert.event_broker.publication_adapter_ri.config.BrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class PublicationAdapterRestController {

    @Autowired
    BrokerConfig config;

    @Autowired
    PublicationAdapterService publicationAdapterService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationAdapterRestController.class);

    @PostMapping
    public ResponseEntity<Object> publish(@RequestBody EventFromPublisher eventFromPublisher) {
        try {
            EventToPublisher eventToPublisher = publicationAdapterService.publish(eventFromPublisher);
            return new ResponseEntity<Object>(eventToPublisher, HttpStatus.CREATED);
        } catch (BrokerException ex) {
            // If error is a BrokerException, the error should already have been logged
            //LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity<Object>(new BrokerExceptionResponse(ex), ex.getHttpStatus());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return new ResponseEntity<Object>(new BrokerExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
