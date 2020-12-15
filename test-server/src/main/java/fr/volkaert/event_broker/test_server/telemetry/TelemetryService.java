package fr.volkaert.event_broker.test_server.telemetry;

import fr.volkaert.event_broker.test_server.model.TestEvent;
import fr.volkaert.event_broker.test_server.model.TestRecord;
import io.micrometer.core.instrument.*;
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

    public synchronized String testStarted(TestEvent event) {
        String msg = "";
        try {
            msg = String.format("Test started. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for testStarted", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_started_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for testStarted", ex);
        }
        return msg;
    }

    public synchronized String testSucceeded(TestEvent event, TestRecord record) {
        String msg = "";
        try {
            msg = String.format("Test succeeded. Record is %s. Event is %s.", record, event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for testSucceeded", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_succeeded_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for testSucceeded", ex);
        }
        return msg;
    }

    public synchronized String testFailed(TestEvent event, TestRecord record) {
        String msg = "";
        try {
            msg = String.format("Test failed. Record is %s. Event is %s.", record, event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for testFailed", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_failed_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for testFailed", ex);
        }
        return msg;
    }

    public synchronized String testStopped(String testId) {
        String msg = "";
        try {
            msg = String.format("Test stopped. Test id is %s.", testId);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for testStopped", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_stopped_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for testStopped", ex);
        }
        return msg;
    }

    public synchronized String publicationAttempted(TestEvent event) {
        String msg = "";
        try {
            msg = String.format("Event publication attempted. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for publicationAttempted", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_event_publication_attempted_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for publicationAttempted", ex);
        }
        return msg;
    }

    public synchronized String publicationSucceeded(TestEvent event, Instant publicationStart) {
        String msg = "";
        try {
            msg = String.format("Event publication succeeded. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for publicationSucceeded", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_event_publication_succeeded_total");
            counter1.increment();

            Instant publicationEnd = Instant.now();
            Timer publicationTimer = meterRegistry.timer("test_event_publication_duration");
            publicationTimer.record(Duration.between(publicationStart, publicationEnd).toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for publicationSucceeded", ex);
        }
        return msg;
    }

    public synchronized String publicationFailed(TestEvent event, Exception exception, Instant publicationStart) {
        String msg = "";
        try {
            msg = String.format("Event publication failed. Exception is `%s`. Event is %s.",
                    (exception != null ? exception.getMessage() : ""),  event);
            LOGGER.error(msg, exception);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for publicationFailed", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_event_publication_failed_total");
            counter1.increment();

            Instant publicationEnd = Instant.now();
            Timer publicationTimer = meterRegistry.timer("test_event_publication_duration");
            publicationTimer.record(Duration.between(publicationStart, publicationEnd).toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for publicationFailed", ex);
        }
        return msg;
    }

    public synchronized String eventReceived(TestEvent event) {
        String msg = "";
        try {
            msg = String.format("Event received. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventReceived", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_event_received_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventReceived", ex);
        }
        return msg;
    }

    public synchronized String eventMissingOrUnordered(TestEvent event) {
        String msg = "";
        try {
            msg = String.format("Event is missing or unordered. Event is %s.", event);
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventMissingOrUnordered", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_event_missing_or_unordered_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventMissingOrUnordered", ex);
        }
        return msg;
    }

    public synchronized String eventCountsMismatch(TestEvent event, long expectedCount, long actualCount) {
        String msg = "";
        try {
            msg = String.format("Event counts mismatch. Expected count is %d. Actual count is %d. Last Event is %s.", expectedCount, actualCount, event);
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventCountsMismatch", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("test_event_counts_mismatch_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventCountsMismatch", ex);
        }
        return msg;
    }

    public synchronized void  recordEventRoundtripDuration(TestEvent event, long eventRoundtripDurationInMillis) {
        try {
            Timer roundtripDurationTimer = meterRegistry.timer("test_event_roundtrip_duration", Tags.of("testId", event.getPayload().getTestId()));
            roundtripDurationTimer.record(eventRoundtripDurationInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for recordEventRoundtripDuration", ex);
        }
    }

}
