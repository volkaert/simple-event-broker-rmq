package fr.volkaert.event_broker.publication_adapter_ri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerConfigForPublicationAdapter {

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)

    private long connectTimeoutInSecondsForPublicationManager;
    private long readTimeoutInSecondsForPublicationManager;

    private String publicationManagerUrl;
    private String authClientIdForPublicationManager;
    private String authClientSecretForPublicationManager;
}
