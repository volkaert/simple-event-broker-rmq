package fr.volkaert.event_broker.catalog_adapter_client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

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
        if (! StringUtils.isEmpty(authClientIdForCatalogAdapter) && ! StringUtils.isEmpty(authClientSecretForCatalogAdapter))
            return builder
                    .basicAuthentication(authClientIdForCatalogAdapter, authClientSecretForCatalogAdapter)
                    .build();
        else
            return builder
                    .build();
    }
}
