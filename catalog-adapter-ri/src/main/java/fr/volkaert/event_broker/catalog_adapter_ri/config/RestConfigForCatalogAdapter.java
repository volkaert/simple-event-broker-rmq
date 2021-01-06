package fr.volkaert.event_broker.catalog_adapter_ri.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class RestConfigForCatalogAdapter {

    @Value("${broker.auth-client-id-for-catalog}")
    String authClientIdForCatalog;

    @Value("${broker.auth-client-secret-for-catalog}")
    String authClientSecretForCatalog;

    @Bean
    @Qualifier("RestTemplateForCatalog")
    //@LoadBalanced
    public RestTemplate restTemplateForCatalog(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .basicAuthentication(authClientIdForCatalog, authClientSecretForCatalog)
                .build();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
