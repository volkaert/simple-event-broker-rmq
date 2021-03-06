package fr.volkaert.event_broker.subscription_adapter_ri.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerConfigForSubscriptionAdapter {

    private String componentTypeName;   // Useful for metrics (to group them by component type)
    private String componentInstanceId; // Useful for metrics (to distinguish instances of the same component type)

    private long connectTimeoutInSecondsForWebhooks;
    private long readTimeoutInSecondsForWebhooks;

    private String oauth2TokenEndpoint;
    private String oauth2ClientId;
    private String oauth2ClientSecret;
    private long connectTimeoutInSecondsForOAuth2Issuer;
    private long readTimeoutInSecondsForOAuth2Issuer;

}