package fr.volkaert.event_broker.catalog_cache;

import fr.volkaert.event_broker.catalog_adapter_client.CatalogAdapterClient;
import fr.volkaert.event_broker.error.BrokerException;
import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class CatalogCache {

    protected CatalogBackendForCache backend;
    public void setBackend(CatalogBackendForCache backend) {
        this.backend = backend;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogAdapterClient.class);

    private final Object eventTypesLock = new Object();
    private List<EventType> eventTypesList = new ArrayList<>();
    private final Map<String, EventType> eventTypesMap = new ConcurrentHashMap<>();
    private boolean eventTypesInitDone = false;

    private final Object publicationsLock = new Object();
    private List<Publication> publicationsList = new ArrayList<>();
    private final Map<String, Publication> publicationsMap = new ConcurrentHashMap<>();
    private boolean publicationsInitDone = false;

    private final Object subscriptionsLock = new Object();
    private List<Subscription> subscriptionsList = new ArrayList<>();
    private final Map<String, Subscription> subscriptionsMap = new ConcurrentHashMap<>();
    private boolean subscriptionsInitDone = false;

    public CatalogCache() {
    }

    @Scheduled(fixedDelay = 60000)
    public void refreshCaches() {
        if (backend == null) return;
        LOGGER.info("Refreshing catalog caches");
        try {
            refreshEventTypesCache();
        } catch (Exception ex) {
            LOGGER.error("Error while refreshing event types cache", ex);
        }
        try {
            refreshPublicationsCache();
        } catch (Exception ex) {
            LOGGER.error("Error while refreshing publications cache", ex);
        }
        try {
            refreshSubscriptionsCache();
        } catch (Exception ex) {
            LOGGER.error("Error while refreshing subscriptions cache", ex);
        }
    }

    private void refreshEventTypesCache() {
        synchronized (eventTypesLock) {
            List<EventType> newEventTypes = backend.getEventTypes();
            eventTypesList = newEventTypes;
            eventTypesMap.clear();  // clear must be after backend.getEventTypes() because if this operation throws an error we want to keep the current values in the cache
            for (EventType eventType : newEventTypes) {
                eventTypesMap.put(eventType.getCode(), eventType);
            }
            eventTypesInitDone = true;
        }
    }

    private void refreshPublicationsCache() {
        synchronized (publicationsLock) {
            List<Publication> newPublications = backend.getPublications();
            publicationsList = newPublications;
            publicationsMap.clear();  // clear must be after backend.getPublications() because if this operation throws an error we want to keep the current values in the cache
            for (Publication publication : newPublications) {
                publicationsMap.put(publication.getCode(), publication);
            }
            publicationsInitDone = true;
        }
    }

    private void refreshSubscriptionsCache() {
        synchronized (subscriptionsLock) {
            List<Subscription> newSubscriptions = backend.getSubscriptions();
            subscriptionsList = newSubscriptions;
            subscriptionsMap.clear();  // clear must be after backend.getSubscriptions() because if this operation throws an error we want to keep the current values in the cache
            for (Subscription subscription : newSubscriptions) {
                subscriptionsMap.put(subscription.getCode(), subscription);
            }
            subscriptionsInitDone = true;
        }
    }

    public List<EventType> getEventTypes() {
        synchronized (eventTypesLock) {
            if (!eventTypesInitDone) {
                refreshEventTypesCache();
            }
        }
        return eventTypesList;
    }

    public EventType getEventType(String code) {
        synchronized (eventTypesLock) {
            if (!eventTypesInitDone) {
                refreshEventTypesCache();
            }
        }
        return eventTypesMap.get(code);
    }

    public EventType getEventTypeOrThrowException(String code) {
        EventType eventType = getEventType(code);
        if (eventType == null) {
            String msg = String.format("Invalid event type code '%s'", code);
            LOGGER.error(msg);
            throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
        return eventType;
    }

    public List<Publication> getPublications() {
        synchronized (publicationsLock) {
            if (!publicationsInitDone) {
                refreshPublicationsCache();
            }
        }
        return publicationsList;
    }

    public Publication getPublication(String code) {
        synchronized (publicationsLock) {
            if (!publicationsInitDone) {
                refreshPublicationsCache();
            }
        }
        return publicationsMap.get(code);
    }

    public Publication getPublicationOrThrowException(String code) {
        Publication publication = getPublication(code);
        if (publication == null) {
            String msg = String.format("Invalid publication code '%s'", code);
            LOGGER.error(msg);
            throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
        return publication;
    }

    public List<Subscription> getSubscriptions() {
        synchronized (subscriptionsLock) {
            if (!subscriptionsInitDone) {
                refreshSubscriptionsCache();
            }
        }
        return subscriptionsList;
    }

    public Subscription getSubscription(String code) {
        synchronized (subscriptionsLock) {
            if (!subscriptionsInitDone) {
                refreshSubscriptionsCache();
            }
        }
        return subscriptionsMap.get(code);
    }

    public Subscription getSubscriptionOrThrowException(String code) {
        Subscription subscription = getSubscription(code);
        if (subscription == null) {
            String msg = String.format("Invalid subscription code '%s'", code);
            LOGGER.error(msg);
            throw new BrokerException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
        return subscription;
    }
}
