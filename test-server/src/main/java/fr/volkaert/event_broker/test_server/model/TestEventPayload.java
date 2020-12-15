package fr.volkaert.event_broker.test_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEventPayload {

    private String testId;
    private Instant testTimestamp;

    private String message;

    private Instant currentEventTimestamp;

    private boolean isFirstEvent;
    //private Instant firstEventTimestamp;

    private boolean isLastEvent;
    //private Instant lastEventTimestamp;
    private Long expectedCount;

    private Long index; // to check delivery order
}
