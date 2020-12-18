package fr.volkaert.event_broker.catalog_adapter;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class CatalogAdapterApplication {

    @Value("${broker.auth-client-id-for-catalog}")
    String authClientIdForCatalog;

    @Value("${broker.auth-client-secret-for-catalog}")
    String authClientSecretForCatalog;

    public static void main(String[] args) {
        SpringApplication.run(CatalogAdapterApplication.class, args);
    }

    @Bean
    @Qualifier("RestTemplateForCatalog")
    @LoadBalanced
    public RestTemplate restTemplateForCatalog(RestTemplateBuilder builder) {
        return builder
                .basicAuthentication(authClientIdForCatalog, authClientSecretForCatalog)
                .build();
    }
}

