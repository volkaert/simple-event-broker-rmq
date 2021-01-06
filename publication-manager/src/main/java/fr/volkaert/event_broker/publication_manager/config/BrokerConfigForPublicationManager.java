package fr.volkaert.event_broker.publication_manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerConfigForPublicationManager {

    private String rabbitMQHost;
    private int rabbitMQPort;
    private String rabbitMQUsername;
    private String rabbitMQPassword;

    private boolean mirroringActive;

    private String rabbitMQHostForMirroring;
    private int rabbitMQPortForMirroring;
    private String rabbitMQUsernameForMirroring;
    private String rabbitMQPasswordForMirroring;

    private String catalogAdapterUrl;

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)

    private long defaultTimeToLiveInSeconds;
    private long maxTimeToLiveInSeconds;
}
