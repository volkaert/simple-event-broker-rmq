package fr.volkaert.event_broker.catalog_cache;

import fr.volkaert.event_broker.model.EventType;
import fr.volkaert.event_broker.model.Publication;
import fr.volkaert.event_broker.model.Subscription;

import java.util.List;

public interface CatalogBackendForCache {
    List<EventType> getEventTypes();
    List<Publication> getPublications();
    List<Subscription> getSubscriptions();
}


