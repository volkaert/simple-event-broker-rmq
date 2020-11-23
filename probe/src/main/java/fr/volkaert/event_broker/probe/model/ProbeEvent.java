package fr.volkaert.event_broker.probe.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProbeEvent extends BrokerEvent {

    private ProbeEventPayload payload;
}
