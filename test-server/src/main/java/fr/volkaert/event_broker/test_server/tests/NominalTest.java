package fr.volkaert.event_broker.test_server.tests;

import fr.volkaert.event_broker.test_server.model.BrokerEvent;
import fr.volkaert.event_broker.test_server.model.TestEvent;
import fr.volkaert.event_broker.test_server.model.TestEventPayload;
import fr.volkaert.event_broker.test_server.model.TestRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping(value = "/tests/nominal")
public class NominalTest extends  AbstractTest {

    @Autowired
    @Qualifier("ExecutorServiceForTests")
    ExecutorService executorService;

    @GetMapping(value = "/run", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> run(
            @RequestParam(required = false) String testId,
            @RequestParam(required = false) String publicationCode,
            @RequestParam(required = false) Long timeToLiveInSeconds,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false, defaultValue = "1") long n,
            @RequestParam(required = false, defaultValue = "0") long pause,
            @RequestParam(required = false, defaultValue = "false") boolean sync,
            @RequestHeader HttpHeaders httpHeaders) {

        if (testId == null) testId = UUID.randomUUID().toString();
        lastTestId = testId;

        if (sync) {
            privateRun(testId, publicationCode, timeToLiveInSeconds, channel, n, pause, httpHeaders);
            return ResponseEntity.ok("Events published for test " + testId + "\n");
        }
        else {
            final String finalTestId = testId;
            executorService.execute(() -> {
                privateRun(finalTestId, publicationCode, timeToLiveInSeconds, channel, n, pause, httpHeaders);
            });
            return ResponseEntity.ok("Test " + testId + " started\n");
        }
    }

    private void privateRun(String testId, String publicationCode, Long timeToLiveInSeconds, String channel,
                            long n, long pauseInMillis, HttpHeaders httpHeaders) {
        String msg = String.format("/tests/nominal/run called (headers are %s)", n, httpHeaders);
        LOGGER.info(msg);

        Instant testTimestamp = Instant.now();

        TestRecord record = new TestRecord();
        record.setTestId(testId);
        record.setTestTimestamp(testTimestamp);
        record.setHostName(getHostName());
        recordingService.addRecord(record);

        for (long index = 1; index <= n; index++) {
            if (stopTestRequested(testId))
                return; // *** PAY ATTENTION ***: There is a RETURN here !!!

            while (suspendTestRequested(testId)) {
                pause(1000);
                if (stopTestRequested(testId))
                    return; // *** PAY ATTENTION ***: There is a RETURN here !!!
            }

            Instant eventTimestamp = Instant.now();

            TestEvent event = new TestEvent();
            event.setBusinessId(testId);
            event.setPublicationCode(publicationCode != null ? publicationCode : config.getPublicationCode());
            event.setTimeToLiveInSeconds(timeToLiveInSeconds != null ? timeToLiveInSeconds : config.getTimeToLiveInSeconds());
            event.setChannel(channel != null ? channel : config.getChannel());

            event.setPayload(new TestEventPayload());
            event.getPayload().setTestId(testId);
            event.getPayload().setTestTimestamp(testTimestamp);
            event.getPayload().setEventTimestamp(eventTimestamp);
            event.getPayload().setIndex(index);

            if (index == 1) {
                telemetryService.testStarted(event);
                event.getPayload().setFirstEvent(true);
            }
            if (index == n) {
                event.getPayload().setLastEvent(true);
                event.getPayload().setExpectedCount(n);
            }

            LOGGER.debug("Event to publish: {}", event);
            BrokerEvent eventReturnedByBroker = publishTestEvent(event);
            LOGGER.debug("Event returned by the Broker: {}", eventReturnedByBroker);

            if (index < n && pauseInMillis > 0) {
                pause(pauseInMillis);
            }
        }
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public synchronized ResponseEntity<Void> onTestEventReceived(@RequestBody TestEvent event, @RequestHeader HttpHeaders httpHeaders) {
        telemetryService.eventReceived(event);

        String testId = event.getPayload().getTestId();
        lastTestId = testId;

        TestDataForSubscribers testData = getTestDataForSubscribers(testId);

        if (! testData.statusToReturnForWebhook.is2xxSuccessful()) {
            return ResponseEntity.status(testData.statusToReturnForWebhook).build();
        }

        // check delivery order
        TestEvent lastReceivedEvent = testData.lastReceivedEvent;
        if (lastReceivedEvent != null) {
            if (event.getPayload().getIndex() != lastReceivedEvent.getPayload().getIndex() + 1) {
                telemetryService.eventMissingOrUnordered(event);
            }
        }
        testData.lastReceivedEvent = event;

        // inc event counter
        ++testData.receivedEventsCount;

        // Cumulate the round trip duration to make an average at the end of the test
        long eventRoundtripDurationInMillis = Duration.between(event.getPayload().getEventTimestamp(), Instant.now()).toMillis();
        testData.sumOfEventRoundtripDurationInMillis = testData.sumOfEventRoundtripDurationInMillis + eventRoundtripDurationInMillis;

        if (event.getPayload().isLastEvent()) {
            if (event.getPayload().getExpectedCount() != testData.receivedEventsCount) {
                TestRecord record = getTestRecord(event, testData.receivedEventsCount, testData.sumOfEventRoundtripDurationInMillis);
                record.setFinished(true);
                record.setSucceeded(false);
                record.setReasonIfFailed("Event counts mismatch");
                recordTest(record);

                telemetryService.eventCountsMismatch(event, event.getPayload().getExpectedCount(), testData.receivedEventsCount);
                telemetryService.testFailed(event, record);
            }
            else {
                TestRecord record = getTestRecord(event, testData.receivedEventsCount, testData.sumOfEventRoundtripDurationInMillis);
                record.setFinished(true);
                record.setSucceeded(true);
                recordTest(record);

                telemetryService.testSucceeded(event, record);
            }
        }

        if (testData.pauseInMillis > 0) {
            pause(testData.pauseInMillis);

        }
        return ResponseEntity.status(testData.statusToReturnForWebhook).build();
    }
}
