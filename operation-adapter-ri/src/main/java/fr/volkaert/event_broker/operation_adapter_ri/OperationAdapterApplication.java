package fr.volkaert.event_broker.operation_adapter_ri;

import fr.volkaert.event_broker.operation_adapter_ri.config.BrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@ComponentScan("fr.volkaert")  // Required because some components/services are not in the same project !
public class OperationAdapterApplication {

    @Autowired
    BrokerConfig config;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationAdapterApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OperationAdapterApplication.class, args);
    }

    @Bean
    @Qualifier("RestTemplateForOperationManager")
    @LoadBalanced
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
