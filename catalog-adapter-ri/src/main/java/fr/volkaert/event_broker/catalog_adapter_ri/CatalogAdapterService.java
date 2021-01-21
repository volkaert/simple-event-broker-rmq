package fr.volkaert.event_broker.catalog_adapter_ri;

import fr.volkaert.event_broker.catalog_cache.CatalogBackendForCache;
import fr.volkaert.event_broker.catalog_cache.CatalogCache;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Configuration
public class CatalogAdapterService {

    @Value("${broker.catalog-url}")
    String catalogUrl;

    @Autowired
    @Qualifier("RestTemplateForCatalog")
    RestTemplate restTemplate;

    @Autowired
    CatalogCache cache;

    protected class RemoteCatalogBackendForCache implements CatalogBackendForCache {
        @Override
        public List<EventType> getEventTypes() {
            ResponseEntity<List<EventType>> responseEntity = restTemplate.exchange(catalogUrl + "/catalog/event-types", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<EventType>>() {});
            return responseEntity.getBody();
        }

        @Override
        public List<Publication> getPublications() {
            ResponseEntity<List<Publication>> responseEntity = restTemplate.exchange(catalogUrl + "/catalog/publications", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Publication>>() {});
            return responseEntity.getBody();
        }

        @Override
        public List<Subscription> getSubscriptions() {
            ResponseEntity<List<Subscription>> responseEntity = restTemplate.exchange(catalogUrl + "/catalog/subscriptions", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Subscription>>() {});
            return responseEntity.getBody();
        }
    }
    @PostConstruct
    public void init() {
        if (! StringUtils.isEmpty(catalogUrl)) {
            cache.setBackend(new RemoteCatalogBackendForCache());
        };
    }

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
