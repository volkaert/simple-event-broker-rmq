package fr.volkaert.event_broker.probe.telemetry;

import fr.volkaert.event_broker.probe.model.ProbeEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class TelemetryService {

    @Autowired
    MeterRegistry meterRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryService.class);

    public synchronized String probePublicationAttempted(ProbeEvent event) {
        String msg = "";
        try {
            msg = String.format("Probe event publication attempted. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probePublicationAttempted", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_publications_attempted_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probePublicationAttempted", ex);
        }
        return msg;
    }

    public synchronized String probePublicationSucceeded(ProbeEvent event, Instant publicationStart) {
        String msg = "";
        try {
            msg = String.format("Probe event publication succeeded. Event is %s.", event);
            LOGGER.info(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probePublicationSucceeded", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_publications_succeeded_total");
            counter1.increment();

            Instant publicationEnd = Instant.now();
            Timer publicationTimer = meterRegistry.timer("probe_event_publication_duration");
            publicationTimer.record(Duration.between(publicationStart, publicationEnd).toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probePublicationSucceeded", ex);
        }
        return msg;
    }

    public synchronized String probePublicationFailed(ProbeEvent event, Exception exception, Instant publicationStart) {
        String msg = "";
        try {
            msg = String.format("Probe event publication failed. Exception is `%s`. Event is %s.",
                    (exception != null ? exception.getMessage() : ""),  event);
            LOGGER.error(msg, exception);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probePublicationFailed", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_publications_failed_total");
            counter1.increment();

            Instant publicationEnd = Instant.now();
            Timer publicationTimer = meterRegistry.timer("probe_event_publication_duration");
            publicationTimer.record(Duration.between(publicationStart, publicationEnd).toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probePublicationFailed", ex);
        }
        return msg;
    }

    public synchronized String probeEventReceived(ProbeEvent event) {
        String msg = "";
        try {
            msg = String.format("Probe event received. Event is %s.", event);
            LOGGER.info(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probeEventReceived", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_received_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probeEventReceived", ex);
        }
        return msg;
    }

    public synchronized String probeEventReceivedAfterThreshold(ProbeEvent event, long roundTripDurationInMillis, long thresholdInSeconds) {
        String msg = "";
        try {
            msg = String.format("Probe event received after threshold. RoundTripDurationInMillis is %s. ThresholdInSeconds is %s. Event is %s.",
                    roundTripDurationInMillis, thresholdInSeconds, event);
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probeEventReceivedAfterThreshold", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_received_after_threshold_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probeEventReceivedAfterThreshold", ex);
        }
        return msg;
    }

    public synchronized String probeEventReceivedBeforeThreshold(ProbeEvent event, long roundTripDurationInMillis, long thresholdInSeconds) {
        String msg = "";
        try {
            msg = String.format("Probe event received before threshold. RoundTripDurationInMillis is %s. ThresholdInSeconds is %s. Event is %s.",
                    roundTripDurationInMillis, thresholdInSeconds, event);
            LOGGER.info(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probeEventReceivedBeforeThreshold", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_received_before_threshold_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probeEventReceivedBeforeThreshold", ex);
        }
        return msg;
    }

    public synchronized String probeEventNotReceivedBeforeThreshold(ProbeEvent event, long durationInMillisSinceLastPublication, long thresholdInSeconds) {
        String msg = "";
        try {
            msg = String.format("Probe event NOT received before threshold. DurationInMillisSinceLastPublication is %s. ThresholdInSeconds is %s. Published probe event was %s.",
                    durationInMillisSinceLastPublication, thresholdInSeconds, event);
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for probeEventNotReceivedBeforeThreshold", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("probe_event_not_received_before_threshold_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for probeEventNotReceivedBeforeThreshold", ex);
        }
        return msg;
    }

    public synchronized String publishedProbeEventAndReceivedProbeEventDoNotMatch(ProbeEvent lastProbeEventPublished, ProbeEvent lastProbeEventReceived) {
        String msg = "";
        try {
            msg = String.format("Published probe event and received probe event do NOT match. Published probe event is %s. Received probe event is %s.",
                    lastProbeEventPublished, lastProbeEventReceived);
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for publishedProbeEventAndReceivedProbeEventDoNotMatch", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("published_probe_event_and_received_probe_event_not_matching_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for publishedProbeEventAndReceivedProbeEventDoNotMatch", ex);
        }
        return msg;
    }
}
