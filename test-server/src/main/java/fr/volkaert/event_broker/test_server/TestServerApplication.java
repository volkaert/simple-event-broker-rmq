package fr.volkaert.event_broker.test_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class TestServerApplication {

    @Autowired
    TestServerConfig config;

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TestServerApplication.class, args);
    }

    @Bean
    @Qualifier("RestTemplateForPublication")
    @LoadBalanced
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


    @Bean
    @Qualifier("ExecutorServiceForTests")
    public ExecutorService createExecutorServiceForTests() {
        return Executors.newFixedThreadPool(config.getThreadPoolSize());

    }
}
