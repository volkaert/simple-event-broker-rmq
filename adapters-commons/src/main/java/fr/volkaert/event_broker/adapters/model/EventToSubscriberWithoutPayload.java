package fr.volkaert.event_broker.adapters.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class EventToSubscriberWithoutPayload {

    // Attributes filled by the publisher
    protected String businessId;
    protected String publicationCode;
    //protected Object payload;
    protected Long timeToLiveInSeconds;
    protected String channel;

    // Attributes filled by the broker
    protected String id;
    protected Instant creationDate;
    protected Instant expirationDate;
    protected String eventTypeCode;
    protected String subscriptionCode;
    protected String secret;

    protected  boolean redelivered;
    protected int redeliveryCount;
}
