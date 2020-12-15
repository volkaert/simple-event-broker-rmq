package fr.volkaert.event_broker.test_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRecord {

    private String testId;
    private Instant testTimestamp;
    private boolean finished;
    private boolean succeeded;
    private String reasonIfFailed;
    private Long durationInSeconds;
    private Long averageRoundtripDurationInMillis;
    private Long expectedCount;
    private Long actualCount;

    private String hostName;
}
