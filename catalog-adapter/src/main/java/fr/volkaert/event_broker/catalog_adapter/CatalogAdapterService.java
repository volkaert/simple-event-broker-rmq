package fr.volkaert.event_broker.catalog_adapter;

import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CatalogAdapterService {

    @Value("${broker.catalog-url}")
    String catalogUrl;

    @Autowired
    @Qualifier("RestTemplateForCatalog")
    RestTemplate restTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogAdapterService.class);

    public List<EventType> getEventTypes() {
        ResponseEntity<List<EventType>> responseEntity = restTemplate.exchange(catalogUrl + "/catalog/event-types", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EventType>>() {});
        return responseEntity.getBody();
    }

    public EventType getEventType(String code) {
        ResponseEntity<EventType> responseEntity = restTemplate.getForEntity(catalogUrl + "/catalog/event-types/" + code, EventType.class);
        return responseEntity.getBody();
    }

    public List<Publication> getPublications() {
        ResponseEntity<List<Publication>> responseEntity = restTemplate.exchange(catalogUrl + "/catalog/publications", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Publication>>() {});
        return responseEntity.getBody();
    }

    public Publication getPublication(String code) {
        ResponseEntity<Publication> responseEntity = restTemplate.getForEntity(catalogUrl + "/catalog/publications/" + code, Publication.class);
        return responseEntity.getBody();
    }

    public List<Subscription> getSubscriptions() {
        ResponseEntity<List<Subscription>> responseEntity = restTemplate.exchange(catalogUrl + "/catalog/subscriptions", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Subscription>>() {});
        return responseEntity.getBody();
    }

    public Subscription getSubscription(String code) {
        ResponseEntity<Subscription> responseEntity = restTemplate.getForEntity(catalogUrl + "/catalog/subscriptions/" + code, Subscription.class);
        return responseEntity.getBody();
    }
}
