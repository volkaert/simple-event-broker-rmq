package fr.volkaert.event_broker.operation_adapter_ri.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class RestConfigForOperationAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfigForOperationAdapter.class);

    @Autowired
    BrokerConfigForOperationAdapter config;

    @Bean
    @Qualifier("RestTemplateForOperationManager")
    //@LoadBalanced
    public RestTemplate restTemplateForOperationManager(RestTemplateBuilder builder) {
        LOGGER.info("Timeouts for Operation Manager: connect={}, read={}",
                config.getConnectTimeoutInSecondsForOperationManager(), config.getReadTimeoutInSecondsForOperationManager());
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(config.getConnectTimeoutInSecondsForOperationManager()))
                .setReadTimeout(Duration.ofSeconds(config.getReadTimeoutInSecondsForOperationManager()))
                .basicAuthentication(config.getAuthClientIdForOperationManager(), config.getAuthClientSecretForOperationManager())
                .build();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
