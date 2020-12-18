package fr.volkaert.event_broker.catalog_adapter.availability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CatalogAdapterReadinessHealthIndicator implements CompositeHealthContributor {

    @Value("${broker.catalog-url}")
    String catalogUrl;

    @Autowired
    @Qualifier("RestTemplateForCatalog")
    RestTemplate restTemplateForCatalog;

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogAdapterReadinessHealthIndicator.class);

    private Map<String, HealthContributor> contributors = new LinkedHashMap<>();

    public CatalogAdapterReadinessHealthIndicator() {
        this.contributors.put("catalogReadiness", new CatalogReadinessHealthIndicator());
    }

    @Override
    public HealthContributor getContributor(String name) {
        return contributors.get(name);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return contributors.entrySet().stream()
                .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue())).iterator();
    }

    class CatalogReadinessHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            LOGGER.debug("Checking Catalog readiness state");
            String readinessUrlForCatalog = catalogUrl + "/actuator/health/readiness";
            try {
                ResponseEntity<Void> response = restTemplateForCatalog.getForEntity(readinessUrlForCatalog, Void.class);
                if (response.getStatusCode().is2xxSuccessful())
                    return Health.up().build();
                else
                    return Health.down().withDetail("statusCode", response.getStatusCode()).build();
            } catch (Exception ex) {
                LOGGER.error("Exception while checking CatalogAdapter readiness state", ex);
                return Health.down().withDetail("exception", ex).build();
            }
        }
    }
}

