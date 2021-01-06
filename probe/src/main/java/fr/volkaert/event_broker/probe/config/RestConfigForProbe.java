package fr.volkaert.event_broker.probe.config;

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
public class RestConfigForProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfigForProbe.class);

    @Autowired
    ProbeConfig config;

    @Bean
    @Qualifier("RestTemplateForPublication")
    //@LoadBalanced
    public RestTemplate restTemplateForPublication(RestTemplateBuilder builder) {
        LOGGER.info("Timeouts for Publication: connect={}, read={}",
                config.getConnectTimeoutInSeconds(), config.getReadTimeoutInSeconds());
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(config.getConnectTimeoutInSeconds()))
                .setReadTimeout(Duration.ofSeconds(config.getReadTimeoutInSeconds()))
                .build();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
