package fr.volkaert.event_broker.probe;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "probe")
@Data
public class ProbeConfig {

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)

    private long connectTimeoutInSeconds;
    private long readTimeoutInSeconds;

    private String publicationUrl;

    private String publicationCode;
    private long timeToLiveInSeconds;
    private long thresholdInSeconds;
}
