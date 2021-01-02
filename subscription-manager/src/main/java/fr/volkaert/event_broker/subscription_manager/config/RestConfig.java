package fr.volkaert.event_broker.subscription_manager.config;

import fr.volkaert.event_broker.subscription_manager.SubscriptionManagerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class RestConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfig.class);

    @Autowired
    BrokerConfig config;

    @Bean
    @Qualifier("RestTemplateForSubscriptionAdapter")
    //@LoadBalanced
    public RestTemplate restTemplateForSubscriptionAdapter(RestTemplateBuilder builder) {
        LOGGER.info("Timeouts for Subscription Adapter: connect={}, read={}",
                config.getConnectTimeoutInSecondsForSubscriptionAdapter(), config.getReadTimeoutInSecondsForSubscriptionAdapter());
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(config.getConnectTimeoutInSecondsForSubscriptionAdapter()))
                .setReadTimeout(Duration.ofSeconds(config.getReadTimeoutInSecondsForSubscriptionAdapter()))
                .build();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
