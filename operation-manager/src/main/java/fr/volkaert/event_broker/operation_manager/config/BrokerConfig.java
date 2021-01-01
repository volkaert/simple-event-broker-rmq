package fr.volkaert.event_broker.operation_manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerConfig {

    private String rabbitMQHost;
    private int rabbitMQPort;
    private String rabbitMQPortForHttpApi;
    private String rabbitMQUsername;
    private String rabbitMQPassword;

    private String catalogAdapterUrl;

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)
}
