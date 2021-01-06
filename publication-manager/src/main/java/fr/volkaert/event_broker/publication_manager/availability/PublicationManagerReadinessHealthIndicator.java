package fr.volkaert.event_broker.publication_manager.availability;

import fr.volkaert.event_broker.publication_manager.config.BrokerConfigForPublicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PublicationManagerReadinessHealthIndicator implements CompositeHealthContributor {

    @Autowired
    BrokerConfigForPublicationManager config;

    @Autowired
    @Qualifier("RestTemplateForCatalogAdapter")
    RestTemplate restTemplateForCatalogAdapter;

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationManagerReadinessHealthIndicator.class);

    private Map<String, HealthContributor> contributors = new LinkedHashMap<>();

    public PublicationManagerReadinessHealthIndicator() {
        this.contributors.put("catalogAdapterReadiness", new CatalogAdapterReadinessHealthIndicator());
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

    class CatalogAdapterReadinessHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            LOGGER.debug("Checking CatalogAdapter readiness state");
            String readinessUrlForCatalogAdapter = config.getCatalogAdapterUrl() + "/actuator/health/readiness";
            try {
                ResponseEntity<Void> response = restTemplateForCatalogAdapter.getForEntity(readinessUrlForCatalogAdapter, Void.class);
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

