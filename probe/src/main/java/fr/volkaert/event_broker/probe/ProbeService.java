package fr.volkaert.event_broker.probe;

import fr.volkaert.event_broker.probe.config.ProbeConfig;
import fr.volkaert.event_broker.probe.model.ProbeEvent;
import fr.volkaert.event_broker.probe.model.ProbeEventPayload;
import fr.volkaert.event_broker.probe.telemetry.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

@EnableScheduling
@RestController
@RequestMapping(value = "/probe", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ProbeService {

    @Autowired
    ProbeConfig config;

    @Autowired
    @Qualifier("RestTemplateForPublication")
    RestTemplate restTemplate;

    @Autowired
    TelemetryService telemetryService;

    ProbeEvent lastProbeEventPublished;
    Instant lastProbeEventPublicationDate;
    ProbeEvent lastProbeEventReceived;
    Instant lastProbeEventReceptionDate;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProbeService.class);

    @Scheduled(fixedDelayString = "${probe.publication-delay-in-millis:60000}", initialDelay = 10000)
    public synchronized void publishProbeEvent() {
        Instant publicationStart = Instant.now();

        ProbeEvent event = new ProbeEvent();
        event.setBusinessId(UUID.randomUUID().toString());
        event.setPublicationCode(config.getPublicationCode());
        event.setTimeToLiveInSeconds(config.getTimeToLiveInSeconds());
        event.setPayload(new ProbeEventPayload("Hello World"));

        lastProbeEventPublished = event;
        lastProbeEventPublicationDate = Instant.now();

        telemetryService.probePublicationAttempted(event);
        try {
            String publicationUrl = config.getPublicationUrl() + "/events";
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ProbeEvent> request = new HttpEntity<>(event, httpHeaders);
            ResponseEntity<ProbeEvent> response = restTemplate.exchange(publicationUrl, HttpMethod.POST, request, ProbeEvent.class);
            if (response.getStatusCode().is2xxSuccessful())
                telemetryService.probePublicationSucceeded(response.getBody(), publicationStart);
            else
                telemetryService.probePublicationFailed(event,  null, publicationStart);
        } catch (Exception ex) {
            telemetryService.probePublicationFailed(event, ex, publicationStart);
        }
    }

    @Scheduled(fixedDelayString = "${probe.check-delay-in-millis:10000}")
    public synchronized void checkProbeEventReceivedBeforeThreshold() {
        if (lastProbeEventPublished != null && lastProbeEventReceived != null) {
            if (lastProbeEventPublished.getBusinessId().equals(lastProbeEventReceived.getBusinessId())) {
                long roundTripDurationInMillis = lastProbeEventReceptionDate.toEpochMilli() - lastProbeEventPublicationDate.toEpochMilli();
                if (roundTripDurationInMillis > config.getThresholdInSeconds() * 1000) {
                    telemetryService.probeEventReceivedAfterThreshold(lastProbeEventReceived, roundTripDurationInMillis, config.getThresholdInSeconds());
                } else {
                    telemetryService.probeEventReceivedBeforeThreshold(lastProbeEventReceived, roundTripDurationInMillis, config.getThresholdInSeconds());
                }
            }
            else {
                telemetryService.publishedProbeEventAndReceivedProbeEventDoNotMatch(lastProbeEventPublished, lastProbeEventReceived);
            }
            lastProbeEventPublished = null;
            lastProbeEventReceived = null;
        }
        else if (lastProbeEventPublished != null && lastProbeEventReceived == null) {
            long durationInMillisSinceLastPublication = Instant.now().toEpochMilli() - lastProbeEventPublicationDate.toEpochMilli();
            if (durationInMillisSinceLastPublication > config.getThresholdInSeconds() * 1000) {
                telemetryService.probeEventNotReceivedBeforeThreshold(lastProbeEventPublished, durationInMillisSinceLastPublication, config.getThresholdInSeconds());
                lastProbeEventPublished = null; // set to null to prevent from entering this case multiple times
            }
        }
        else if (lastProbeEventPublished == null && lastProbeEventReceived != null) {
            // Can happen when the probe event has been received after the threshold (because lastProbeEventPublished can be set to null in the case just above !).
            // We do not need to raise an alert here because the alert has already been raised in the case just above !
            lastProbeEventReceived = null; // set to null to prevent from entering this case multiple times
        }
        else if (lastProbeEventPublished == null && lastProbeEventReceived == null) {
            // Nothing to do here (because probe event received before threshold or because an alert has already been raised in the cases above)
        }
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public synchronized ResponseEntity<Void> onProbeEventReceived(@RequestBody ProbeEvent event, @RequestHeader HttpHeaders httpHeaders) {
        lastProbeEventReceived = event;
        lastProbeEventReceptionDate = Instant.now();
        telemetryService.probeEventReceived(event);
        return ResponseEntity.ok(null);
    }
}
