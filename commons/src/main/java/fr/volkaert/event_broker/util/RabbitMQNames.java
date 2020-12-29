package fr.volkaert.event_broker.util;

public class RabbitMQNames {
    public static String getExchangeNameForEventType(String eventTypeCode) { return "X_" + eventTypeCode; }
    public static String getQueueNameForSubscription(String subscriptionCode) { return "Q_" + subscriptionCode; }
    public static String getDeadLetterExchangeNameForSubscription(String subscriptionCode) { return "DLX_" + subscriptionCode; }
    public static String getDeadLetterQueueNameForSubscription(String subscriptionCode) { return "DLQ_" + subscriptionCode; }
}
