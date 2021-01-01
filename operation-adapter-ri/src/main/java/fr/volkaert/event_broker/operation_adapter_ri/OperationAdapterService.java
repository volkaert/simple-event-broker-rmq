package fr.volkaert.event_broker.operation_adapter_ri;

import com.rabbitmq.http.client.domain.OverviewResponse;
import com.rabbitmq.http.client.domain.QueueInfo;
import fr.volkaert.event_broker.model.EventToSubscriber;
import fr.volkaert.event_broker.model.InflightEvent;
import fr.volkaert.event_broker.operation_adapter_ri.config.BrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Queue;

@Service
public class OperationAdapterService {

    @Autowired
    BrokerConfig config;

    @Autowired
    @Qualifier("RestTemplateForOperationManager")
    RestTemplate restTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationAdapterService.class);

    public EventToSubscriber getNextEventForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/events/next", config.getOperationManagerUrl(), subscriptionCode);
        ResponseEntity<InflightEvent> responseEntity = restTemplate.exchange(query, HttpMethod.GET, null, InflightEvent.class);
        InflightEvent inflightEvent = responseEntity.getBody();
        EventToSubscriber eventToSubscriber = EventToSubscriber.from(inflightEvent);
        return eventToSubscriber;
    }

    public EventToSubscriber deleteNextEventForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/events/next", config.getOperationManagerUrl(), subscriptionCode);
        ResponseEntity<InflightEvent> responseEntity = restTemplate.exchange(query, HttpMethod.DELETE, null, InflightEvent.class);
        InflightEvent inflightEvent = responseEntity.getBody();
        EventToSubscriber eventToSubscriber = EventToSubscriber.from(inflightEvent);
        return eventToSubscriber;
    }

    public void deleteAllEventsForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/events", config.getOperationManagerUrl(), subscriptionCode);
        restTemplate.exchange(query, HttpMethod.DELETE, null, Void.class);
    }

    public EventToSubscriber getNextEventInDeadlLetterQueueForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/dead-letter-queue/events/next", config.getOperationManagerUrl(), subscriptionCode);
        ResponseEntity<InflightEvent> responseEntity = restTemplate.exchange(query, HttpMethod.GET, null, InflightEvent.class);
        InflightEvent inflightEvent = responseEntity.getBody();
        EventToSubscriber eventToSubscriber = EventToSubscriber.from(inflightEvent);
        return eventToSubscriber;
    }

    public EventToSubscriber deleteNextEventInDeadlLetterQueueForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/dead-letter-queue/events/next", config.getOperationManagerUrl(), subscriptionCode);
        ResponseEntity<InflightEvent> responseEntity = restTemplate.exchange(query, HttpMethod.DELETE, null, InflightEvent.class);
        InflightEvent inflightEvent = responseEntity.getBody();
        EventToSubscriber eventToSubscriber = EventToSubscriber.from(inflightEvent);
        return eventToSubscriber;
    }

    public void deleteAllEventsInDeadlLetterQueueForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/dead-letter-queue/events", config.getOperationManagerUrl(), subscriptionCode);
        restTemplate.exchange(query, HttpMethod.DELETE, null, Void.class);
    }

    public QueueInfo getRabbitMQQueueInfoForSubscription(String subscriptionCode) {
        String query = String.format("%s/subscriptions/%s/rabbitmq/queue/info", config.getOperationManagerUrl(), subscriptionCode);
        ResponseEntity<QueueInfo> responseEntity = restTemplate.exchange(query, HttpMethod.GET, null, QueueInfo.class);
        QueueInfo queueInfo = responseEntity.getBody();
        return queueInfo;
    }

    public OverviewResponse getRabbitMQQueueOverview() {
        String query = String.format("%s/rabbitmq/overview", config.getOperationManagerUrl());
        ResponseEntity<OverviewResponse> responseEntity = restTemplate.exchange(query, HttpMethod.GET, null, OverviewResponse.class);
        OverviewResponse overview = responseEntity.getBody();
        return overview;
    }
}
