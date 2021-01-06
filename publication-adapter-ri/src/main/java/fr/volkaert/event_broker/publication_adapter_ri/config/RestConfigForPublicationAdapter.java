package fr.volkaert.event_broker.publication_adapter_ri.config;

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
public class RestConfigForPublicationAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfigForPublicationAdapter.class);

    @Autowired
    BrokerConfigForPublicationAdapter config;

    @Bean
    @Qualifier("RestTemplateForPublicationManager")
    //@LoadBalanced
    public RestTemplate restTemplateForPublicationManager(RestTemplateBuilder builder) {
        LOGGER.info("Timeouts for Publication Manager: connect={}, read={}",
                config.getConnectTimeoutInSecondsForPublicationManager(), config.getReadTimeoutInSecondsForPublicationManager());
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(config.getConnectTimeoutInSecondsForPublicationManager()))
                .setReadTimeout(Duration.ofSeconds(config.getReadTimeoutInSecondsForPublicationManager()))
                .build();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}

