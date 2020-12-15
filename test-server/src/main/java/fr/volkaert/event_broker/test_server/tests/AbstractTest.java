package fr.volkaert.event_broker.test_server.tests;

import fr.volkaert.event_broker.test_server.TestServerConfig;
import fr.volkaert.event_broker.test_server.model.BrokerEvent;
import fr.volkaert.event_broker.test_server.model.TestEvent;
import fr.volkaert.event_broker.test_server.model.TestRecord;
import fr.volkaert.event_broker.test_server.telemetry.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractTest {
    @Autowired
    protected TestServerConfig config;

    @Autowired
    protected TestRecordingService recordingService;

    @Autowired
    @Qualifier("RestTemplateForPublication")
    protected RestTemplate restTemplate;

    @Autowired
    protected TelemetryService telemetryService;

    protected static class TestDataForPublishers {
        boolean stopRequested = false;
        boolean suspensionRequested = false;}
    protected Map<String, TestDataForPublishers> testIdToTestDataForPublishersMap = new ConcurrentHashMap<>();

    protected static class TestDataForSubscribers {
        TestEvent lastReceivedEvent = null;
        long receivedEventsCount = 0;
        long sumOfEventRoundtripDurationInMillis = 0;
        HttpStatus statusToReturnForWebhook = HttpStatus.OK;
        long pauseInMillis = 0;
    }
    protected Map<String, TestDataForSubscribers> testIdToTestDataForSubscribersMap = new ConcurrentHashMap<>();

    protected String lastTestId = null;

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractTest.class);

    protected BrokerEvent publishTestEvent(TestEvent event) {
        Instant publicationStart = Instant.now();

        if (event.getBusinessId() == null)  event.setBusinessId(UUID.randomUUID().toString());
        if (event.getPublicationCode() == null) event.setPublicationCode(config.getPublicationCode());
        if (event.getTimeToLiveInSeconds() == null) event.setTimeToLiveInSeconds(config.getTimeToLiveInSeconds());

        telemetryService.publicationAttempted(event);
        try {
            String publicationUrl = config.getPublicationUrl() + "/events";
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TestEvent> request = new HttpEntity<>(event, httpHeaders);
            ResponseEntity<BrokerEvent> response = restTemplate.exchange(publicationUrl, HttpMethod.POST, request, BrokerEvent.class);
            if (response.getStatusCode().is2xxSuccessful())
                telemetryService.publicationSucceeded(event, publicationStart);
            else
                telemetryService.publicationFailed(event,  null, publicationStart);
            return response.getBody();
        } catch (Exception ex) {
            telemetryService.publicationFailed(event, ex, publicationStart);
            return null;
        }
    }

    protected TestRecord getTestRecord(TestEvent event, long actualCount, long sumOfEventRoundtripDurationInMillis) {
        Instant now = Instant.now();
        TestRecord record = new TestRecord();
        record.setTestId(event.getPayload().getTestId());
        record.setTestTimestamp(event.getPayload().getTestTimestamp());
        Duration testDuration = Duration.between(event.getPayload().getTestTimestamp(), now);
        record.setDurationInSeconds(testDuration.toSeconds());
        long averageRoundtripDurationInMillis = sumOfEventRoundtripDurationInMillis / actualCount;
        record.setAverageRoundtripDurationInMillis(averageRoundtripDurationInMillis);
        record.setExpectedCount(event.getPayload().getExpectedCount());
        record.setActualCount(actualCount);
        record.setHostName(getHostName());
        return record;
    }

    protected void recordTest(TestRecord record) {
        recordingService.addRecord(record);
    }

    @GetMapping(value = "/stop", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> stop(@RequestParam(required = false) String testId, @RequestHeader HttpHeaders httpHeaders) {
        if (testId == null) testId = lastTestId;
        TestDataForPublishers testData = getTestDataForPublishers(testId);
        testData.stopRequested = true;
        telemetryService.testStopped(testId);
        return ResponseEntity.ok("Test " + testId + " will be stopped\n");
    }

    @GetMapping(value = "/suspend", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> suspend(@RequestParam(required = false) String testId, @RequestHeader HttpHeaders httpHeaders) {
        if (testId == null) testId = lastTestId;
        TestDataForPublishers testData = getTestDataForPublishers(testId);
        testData.suspensionRequested = true;
        return ResponseEntity.ok("Test " + testId + " has been suspended. Use the `resume` command to resume the test.\n");
    }

    @GetMapping(value = "/resume", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> resume(@RequestParam(required = false) String testId, @RequestHeader HttpHeaders httpHeaders) {
        if (testId == null) testId = lastTestId;
        TestDataForPublishers testData = getTestDataForPublishers(testId);
        testData.suspensionRequested = false;
        return ResponseEntity.ok("Test " + testId + " has been resumed\n");
    }

    @GetMapping(value = "/accept", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> accept(@RequestParam(required = false) String testId,
                                         @RequestParam(required = false, defaultValue = "201") int status,
                                         @RequestHeader HttpHeaders httpHeaders) {
        if (testId == null) testId = lastTestId;
        TestDataForSubscribers testData = getTestDataForSubscribers(testId);
        testData.statusToReturnForWebhook = HttpStatus.valueOf(status);
        return ResponseEntity.ok("Webhook will return " + testData.statusToReturnForWebhook + " for test " + testId + "\n");
    }

    @GetMapping(value = "/reject", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> reject(@RequestParam(required = false) String testId,
                                         @RequestParam(required = false, defaultValue = "500") int status,
                                         @RequestHeader HttpHeaders httpHeaders) {
        if (testId == null) testId = lastTestId;
        TestDataForSubscribers testData = getTestDataForSubscribers(testId);
        testData.statusToReturnForWebhook = HttpStatus.valueOf(status);
        return ResponseEntity.ok("Webhook will return " + testData.statusToReturnForWebhook + " for test " + testId + "\n");
    }

    @GetMapping(value = "/slowdown", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> slowdown(@RequestParam(required = false) String testId,
                                           @RequestParam(required = false, defaultValue = "10000") long pause,
                                           @RequestHeader HttpHeaders httpHeaders) {
        if (testId == null) testId = lastTestId;
        TestDataForSubscribers testData = getTestDataForSubscribers(testId);
        testData.pauseInMillis = pause;
        return ResponseEntity.ok("Webhook will slow down for " + testData.pauseInMillis + " millis for test " + testId + "\n");
    }

    protected boolean stopTestRequested(String testId) {
        return getTestDataForPublishers(testId).stopRequested;
    }

    protected boolean suspendTestRequested(String testId) {
        return getTestDataForPublishers(testId).suspensionRequested;
    }

    protected TestDataForPublishers getTestDataForPublishers(String testId)  {
        return testIdToTestDataForPublishersMap.computeIfAbsent(testId, x -> new TestDataForPublishers());
    }

    protected TestDataForSubscribers getTestDataForSubscribers(String testId)  {
        return testIdToTestDataForSubscribersMap.computeIfAbsent(testId, x -> new TestDataForSubscribers());
    }

    protected void pause(long pauseInMillis) {
        try {
            Thread.sleep(pauseInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
