package fr.volkaert.event_broker.catalog_adapter_client;

import fr.volkaert.event_broker.catalog_cache.CatalogBackendForCache;
import fr.volkaert.event_broker.catalog_cache.CatalogCache;
import fr.volkaert.event_broker.error.BrokerException;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Configuration
@EnableScheduling
public class CatalogAdapterClient {

    @Value("${broker.catalog-adapter-url:#{null}}")   // can be null for the Catalog and CatalogAdapter modules
    String catalogAdapterUrl;

    @Autowired
    @Qualifier("RestTemplateForCatalogAdapter")
    RestTemplate restTemplate;

    protected class RemoteCatalogBackendForCache implements CatalogBackendForCache {
        @Override
        public List<EventType> getEventTypes() {
            ResponseEntity<List<EventType>> responseEntity = restTemplate.exchange(catalogAdapterUrl + "/catalog/event-types", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<EventType>>() {});
            return responseEntity.getBody();
        }

        @Override
        public List<Publication> getPublications() {
            ResponseEntity<List<Publication>> responseEntity = restTemplate.exchange(catalogAdapterUrl + "/catalog/publications", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Publication>>() {});
            return responseEntity.getBody();
        }

        @Override
        public List<Subscription> getSubscriptions() {
            ResponseEntity<List<Subscription>> responseEntity = restTemplate.exchange(catalogAdapterUrl + "/catalog/subscriptions", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Subscription>>() {});
            return responseEntity.getBody();
        }
    }

    private CatalogCache cache;

    @PostConstruct
    public void init() {
        if (! StringUtils.isEmpty(catalogAdapterUrl)) {
            cache = createCatalogCache();
            cache.setBackend(new RemoteCatalogBackendForCache());
        };
    }

    @Bean
    public CatalogCache createCatalogCache() {
        return new CatalogCache();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogAdapterClient.class);

    public List<EventType> getEventTypes() {
        return cache.getEventTypes();
    }

    public EventType getEventType(String code) {
        return cache.getEventType(code);
    }

    public EventType getEventTypeOrThrowException(String code) {
        return cache.getEventTypeOrThrowException(code);
    }

    public List<Publication> getPublications() {
        return cache.getPublications();
    }

    public Publication getPublication(String code) {
        return cache.getPublication(code);
    }

    public Publication getPublicationOrThrowException(String code) {
        return cache.getPublicationOrThrowException(code);
    }

    public List<Subscription> getSubscriptions() {
        return cache.getSubscriptions();
    }

    public Subscription getSubscription(String code) {
        return cache.getSubscription(code);
    }

    public Subscription getSubscriptionOrThrowException(String code) {
        return cache.getSubscriptionOrThrowException(code);
    }
}
