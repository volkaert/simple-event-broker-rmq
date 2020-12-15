package fr.volkaert.event_broker.test_server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class BrokerEvent {

    private String businessId;
    private String publicationCode;
    private Long timeToLiveInSeconds;
    private String channel;

    private String id;
    private Instant creationDate;
    private Instant expirationDate;
    private String eventTypeCode;
    private String subscriptionCode;
    private String secret;

    private boolean redelivered;
    private int deliveryCount;
}
