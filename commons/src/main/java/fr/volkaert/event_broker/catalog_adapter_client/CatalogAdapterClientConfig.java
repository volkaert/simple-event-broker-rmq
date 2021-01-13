package fr.volkaert.event_broker.catalog_adapter_client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class CatalogAdapterClientConfig {

    @Value("${broker.auth-client-id-for-catalog-adapter:#{null}}")   // can be null for the Catalog and CatalogAdapter modules
    String authClientIdForCatalogAdapter;

    @Value("${broker.auth-client-secret-for-catalog-adapter:#{null}}")   // can be null for the Catalog and CatalogAdapter modules
    String authClientSecretForCatalogAdapter;

    @Bean
    @Qualifier("RestTemplateForCatalogAdapter")
    //@LoadBalanced
    public RestTemplate restTemplateForCatalogAdapter(RestTemplateBuilder builder) {
        RestTemplate restTemplate = null;
        if (! StringUtils.isEmpty(authClientIdForCatalogAdapter) && ! StringUtils.isEmpty(authClientSecretForCatalogAdapter))
            restTemplate = builder
                    .basicAuthentication(authClientIdForCatalogAdapter, authClientSecretForCatalogAdapter)
                    .build();
        else
            restTemplate = builder
                    .build();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
