package fr.volkaert.event_broker.subscription_manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class SubscriptionManagerRestController {

    @Autowired
    SubscriptionManagerService service;

    @PostMapping(value = "/event-processing/activate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> activateEventProcessing() {
        service.activateEventProcessing();
        return ResponseEntity.ok("Event processing is now active\n");
    }

    @PostMapping(value = "/event-processing/deactivate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deactivateEventProcessing() {
        service.deactivateEventProcessing();
        return ResponseEntity.ok("Event processing is now NOT active\n");
    }
}
