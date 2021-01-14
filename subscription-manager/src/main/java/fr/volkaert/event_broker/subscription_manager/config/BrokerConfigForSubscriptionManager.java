package fr.volkaert.event_broker.subscription_manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerConfigForSubscriptionManager {

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)

    private long connectTimeoutInSecondsForSubscriptionAdapter;
    private long readTimeoutInSecondsForSubscriptionAdapter;

    private String rabbitMQHost;
    private int rabbitMQPort;
    private String rabbitMQUsername;
    private String rabbitMQPassword;
    private boolean rabbitMQSSLEnabled;

    private String subscriptionAdapterUrl;
    private String authClientIdForSubscriptionAdapter;
    private String authClientSecretForSubscriptionAdapter;

    private long defaultTimeToLiveInSecondsForWebhookConnectionError;
    private long defaultTimeToLiveInSecondsForWebhookReadTimeoutError;
    private long defaultTimeToLiveInSecondsForWebhookServer5xxError;
    private long defaultTimeToLiveInSecondsForWebhookClient4xxError;
    private long defaultTimeToLiveInSecondsForWebhookAuth401Or403Error;

    private long timeToLiveInSecondsForDeadLetterQueues;    // TTL set during queue declaration so cannot be changed afterwards
    private long timeToLiveInSecondsForDeadLetterMessages;  // TTL set at the message level so can be changed independently of the timeToLiveInSecondsForDeadLetterQueues

    // Cluster size is the number of SubscriptionManager instances and Cluster index is the index of this
    // SubscriptionManager instance within the cluster.
    // Cluster index must  be ***UNIQUE*** within the cluster and must follow the sequence 0, 1... < Cluster size.
    // The SubscriptionManager instance in charge of the management of an event is the instance that meets the
    // criteria 'broker.cluster-index == (sum-of-the-ascii-codes-of-the-chars-of-event-type-code % broker.cluster-size)'.
    // For a given event type, only one instance of SubscriptionManager will manage the events.
    private int clusterSize;
    private int clusterIndex;

    private boolean eventProcessingActive;  //Should be true on the primary datacenter and false on the backup datacenter (to avoid duplicate deliveries)
}
