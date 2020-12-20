package fr.volkaert.event_broker.test_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "test")
@Data
public class TestServerConfig {

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)

    private long connectTimeoutInSeconds;
    private long readTimeoutInSeconds;

    private int threadPoolSize;

    private String publicationUrl;

    private String publicationCode;
    private long timeToLiveInSeconds;
    private String channel;
}
