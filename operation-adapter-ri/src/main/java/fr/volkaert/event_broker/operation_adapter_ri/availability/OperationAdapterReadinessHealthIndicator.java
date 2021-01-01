package fr.volkaert.event_broker.operation_adapter_ri.availability;

import fr.volkaert.event_broker.operation_adapter_ri.config.BrokerConfig;
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
public class OperationAdapterReadinessHealthIndicator implements CompositeHealthContributor {

    @Autowired
    BrokerConfig config;

    @Autowired
    @Qualifier("RestTemplateForOperationManager")
    RestTemplate restTemplateForOperationManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationAdapterReadinessHealthIndicator.class);

    private Map<String, HealthContributor> contributors = new LinkedHashMap<>();

    public OperationAdapterReadinessHealthIndicator() {
        this.contributors.put("operationManagerReadiness", new OperationManagerReadinessHealthIndicator());
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

    class OperationManagerReadinessHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            String readinessUrlForOperationManager = config.getOperationManagerUrl() + "/actuator/health/readiness";
            try {
                ResponseEntity<Void> response = restTemplateForOperationManager.getForEntity(readinessUrlForOperationManager, Void.class);
                if (response.getStatusCode().is2xxSuccessful())
                    return Health.up().build();
                else
                    return Health.down().withDetail("statusCode", response.getStatusCode()).build();
            } catch (Exception ex) {
                LOGGER.error("Exception while checking Operation Manager readiness state", ex);
                return Health.down().withDetail("exception", ex).build();
            }
        }
    }
}

