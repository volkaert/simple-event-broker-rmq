package fr.volkaert.event_broker_test_oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EventToSubscriberWithTestPayload extends EventToSubscriberWithoutPayload {

    private TestPayload payload;
}
