package fr.volkaert.event_broker.util;

public class RabbitMQNames {
    public static String getExchangeNameForEventType(String eventTypeCode) { return "X_" + eventTypeCode; }
    //public static String getExchangeNameForSubscription(String subscriptionCode) { return "X_" + subscriptionCode; }
    public static String getQueueNameForSubscription(String subscriptionCode) { return "Q_" + subscriptionCode; }
    public static String getNameForDeadLetterExchangeForSubscription(String subscriptionCode) { return "DLX_" + subscriptionCode; }
    public static String getNameForDeadLetterQueueForSubscription(String subscriptionCode) { return "DLQ_" + subscriptionCode; }
    //String nameForRetryExchange = "RX_" + subscriptionCode;
    //String nameForRetryQueue = "RQ_" + subscriptionCode;
}
