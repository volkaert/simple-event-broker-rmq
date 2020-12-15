package fr.volkaert.event_broker.test_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEventPayload {

    private String testId;          // present in all events
    private Instant testTimestamp;  // present in all events

    private String message;         // present in all events

    private Instant eventTimestamp; // present in all events
    private Long index;             // present in al events; used to check the delivery order

    private boolean isFirstEvent;   // true is this event is the first event of the test
    private boolean isLastEvent;    // true is this event is the last event of the test (not just the last received, but also the last of the test)

    private Long expectedCount;     // present only in the last event of the test
}
