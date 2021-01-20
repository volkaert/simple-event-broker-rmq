package fr.volkaert.event_broker.telemetry;

import fr.volkaert.event_broker.model.InflightEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TelemetryService {

    @Autowired
    TelemetryConfig config;

    @Autowired
    MeterRegistry meterRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryService.class);

    private final Map<String, AtomicLong> pendingPublicationGauges = new ConcurrentHashMap<>();
    //private final Map<String, AtomicLong> pendingDeliveriesGauges = new ConcurrentHashMap<>();


    private void putInfoInMDC(InflightEvent event, String msgCode, Long duration, TimeUnit durationTimeUnit) {
        MDC.put("component_name", config.getComponentName());
        MDC.put("component_instance_id", config.getComponentInstanceId());

        MDC.remove("event_type_code");
        MDC.remove("publication_code");
        MDC.remove("subscription_code");
        MDC.remove("message_code");
        MDC.remove("duration_in_sec");
        MDC.remove("duration_in_millis");
        MDC.remove("duration");

        MDC.put("message_code", msgCode);
        if (event != null) {
            String eventTypeCode = event.getEventTypeCode();
            if (eventTypeCode != null) {
                MDC.put("event_type_code", eventTypeCode);
            }
            String publicationCode = event.getPublicationCode();
            if (publicationCode != null) {
                MDC.put("publication_code", publicationCode);
            }
            String subscriptionCode = event.getSubscriptionCode();
            if (subscriptionCode != null) {
                MDC.put("subscription_code", subscriptionCode);
            }
        }
        if (duration != null) {
            if (TimeUnit.SECONDS.equals(durationTimeUnit))
                MDC.put("duration_in_sec", duration.toString());
            else if (TimeUnit.MILLISECONDS.equals(durationTimeUnit))
                MDC.put("duration_in_millis", duration.toString());
            else
                MDC.put("duration", duration.toString());
        }
    }

    private void removeInfoInMDC() {
        MDC.remove("component_name");
        MDC.remove("component_instance_id");
        MDC.remove("event_type_code");
        MDC.remove("publication_code");
        MDC.remove("subscription_code");
        MDC.remove("message_code");
        MDC.remove("duration_in_sec");
        MDC.remove("duration_in_millis");
        MDC.remove("duration");
    }



    // PUBLICATION /////////////////////////////////////////////////////////////////////////////////////////////////////


    public synchronized String eventPublicationRequested(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REQUESTED", null, null);
        String msg = "";
        try {
            msg = String.format("Event publication requested. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRequested", ex);
        }

        try {
            Counter counter1 = meterRegistry.counter("event_publications_requested_total");
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRequested", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationRejectedDueToMissingPublicationCode(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REJECTED_MISSING_PUBLICATION_CODE", null, null);
        String msg = "";
        try {
            msg = String.format("Event publication rejected due to missing publication code. Event is %s.", event.toShortLog());
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRejectedDueToMissingPublicationCode", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("event_publications_rejected_total");
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_publications_rejected_due_to_missing_publication_code_total");
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRejectedDueToMissingPublicationCode", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationRejectedDueToInvalidPublicationCode(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REJECTED_INVALID_PUBLICATION_CODE", null, null);
        String msg = "";
        try {
            String publicationCode = event.getPublicationCode();
            msg = String.format("Event publication rejected due to invalid publication code '%s'. Event is %s.", publicationCode, event.toShortLog());
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRejectedDueToInvalidPublicationCode", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("event_publications_rejected_total");
            counter1.increment();
            // do NOT use publicationCode as tag because in case of an unknown publicationCode, its potential values are unbounded !
            Counter counter2 = meterRegistry.counter("event_publications_rejected_due_to_invalid_publication_code_total");
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRejectedDueToInvalidPublicationCode", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationRejectedDueToInactivePublication(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REJECTED_INACTIVE_PUBLICATION", null, null);
        String msg = "";
        try {
            String publicationCode = event.getPublicationCode();
            msg = String.format("Inactive publication '%s'. Event is %s.", publicationCode, event.toShortLog());
            LOGGER.warn(msg); // WARNING and not ERROR !!
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRejectedDueToInactivePublication", ex);
        }
        try {
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_publications_rejected_total");
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_publications_rejected_due_to_inactive_publication_total",
                    Tags.of("publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRejectedDueToInactivePublication", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationRejectedDueToInvalidAuthClientId(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REJECTED_INVALID_AUTH_CLIENT_ID", null, null);
        String msg = "";
        try {
            String authClientId = event.getAuthClientId();
            msg = String.format("Event publication rejected due to invalid auth client id '%s'. Event is %s.", authClientId, event.toShortLog());
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRejectedDueToInvalidAuthClientId", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("event_publications_rejected_total");
            counter1.increment();
            // do NOT use authClientId as tag because in case of an unknown authClientId, its potential values are unbounded !
            Counter counter2 = meterRegistry.counter("event_publications_rejected_due_to_invalid_auth_client_id_total");
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRejectedDueToInvalidAuthClientId", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationRejectedDueToInvalidEventTypeCode(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REJECTED_INVALID_EVENT_TYPE_CODE", null, null);
        String msg = "";
        try {
            String eventTypeCode = event.getEventTypeCode();
            msg = String.format("Event publication rejected due to invalid event type code '%s'. Event is %s.", eventTypeCode, event.toShortLog());
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRejectedDueToInvalidEventTypeCode", ex);
        }
        try {
            Counter counter1 = meterRegistry.counter("event_publications_rejected_total");
            counter1.increment();
            // do NOT use eventTypeCode as tag because in case of an unknown eventTypeCode, its potential values are unbounded !
            Counter counter2 = meterRegistry.counter("event_publications_rejected_due_to_invalid_event_type_code_total");
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRejectedDueToInvalidEventTypeCode", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationRejectedDueToInactiveEventType(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_REJECTED_INACTIVE_EVENT_TYPE", null, null);
        String msg = "";
        try {
            String eventTypeCode = event.getEventTypeCode();
            msg = String.format("Inactive event type '%s'. Event is %s.", eventTypeCode, event.toShortLog());
            LOGGER.warn(msg); // WARNING and not ERROR !!
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationRejectedDueToInactiveEventType", ex);
        }
        try {
            String eventTypeCode = event.getEventTypeCode();
            Counter counter1 = meterRegistry.counter("event_publications_rejected_total");
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_publications_rejected_due_to_inactive_event_type_total",
                    Tags.of("event_type_code", eventTypeCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventPublicationRejectedDueToInactiveEventType", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationAttempted(InflightEvent event) {
        putInfoInMDC(event, "PUBLICATION_ATTEMPTED", null, null);
        String msg = "";
        try {
            msg = String.format("Event publication attempted. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationAttempted", ex);
        }
        try {
            String publicationCode = event.getPublicationCode();
            String eventTypeCode = event.getEventTypeCode();

            Counter counter1 = meterRegistry.counter("event_publications_attempted_total",
                    Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode));
            counter1.increment();

            AtomicLong pendingPublications = pendingPublicationGauges.computeIfAbsent(eventTypeCode + "/" + publicationCode, x -> {
                return meterRegistry.gauge("pending_event_publications",
                        Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode),
                        new AtomicLong(0));
            });
            pendingPublications.incrementAndGet();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventPublicationAttempted", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationSucceeded(InflightEvent event, Instant publicationStart) {
        Instant publicationEnd = Instant.now();
        Long durationInMillis = publicationStart != null ? Duration.between(publicationStart, publicationEnd).toMillis() : null;
        putInfoInMDC(event, "PUBLICATION_SUCCEEDED", durationInMillis, TimeUnit.MILLISECONDS);
        String msg = "";
        try {
            msg = String.format("Event publication succeeded. Event is %s.", event.toShortLog());
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationSucceeded", ex);
        }
        try {
            String publicationCode = event.getPublicationCode();
            String eventTypeCode = event.getEventTypeCode();

            Counter counter1 = meterRegistry.counter("event_publications_succeeded_total",
                    Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode));
            counter1.increment();

            AtomicLong pendingPublications = pendingPublicationGauges.computeIfAbsent(eventTypeCode + "/" + publicationCode, x -> {
                return meterRegistry.gauge("pending_event_publications",
                        Tags.of("publication_code", publicationCode, "event_type_code" , eventTypeCode),
                        new AtomicLong(0));
            });
            pendingPublications.decrementAndGet();

            Timer publicationTimer = meterRegistry.timer("event_publication_duration",
                    Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode));
            publicationTimer.record(durationInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventPublicationSucceeded", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationFailed(InflightEvent event, Exception exception, Instant publicationStart) {
        Instant publicationEnd = Instant.now();
        Long durationInMillis = publicationStart != null ? Duration.between(publicationStart, publicationEnd).toMillis() : null;
        putInfoInMDC(event, "PUBLICATION_FAILED", durationInMillis, TimeUnit.MILLISECONDS);
        String msg = "";
        try {
            msg = String.format("Event publication failed. Exception is `%s`. Event is %s.",
                    (exception != null ? exception.getMessage() : ""),  event.toShortLog());
            LOGGER.error(msg, exception);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationFailed", ex);
        }
        try {
            String publicationCode = event.getPublicationCode();
            String eventTypeCode = event.getEventTypeCode();

            Counter counter1 = meterRegistry.counter("event_publications_failed_total",
                    Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode));
            counter1.increment();

            AtomicLong pendingPublications = pendingPublicationGauges.computeIfAbsent(eventTypeCode + "/" + publicationCode, x -> {
                return meterRegistry.gauge("pending_event_publications",
                        Tags.of("publication_code", publicationCode, "event_type_code" , eventTypeCode),
                        new AtomicLong(0));
            });
            pendingPublications.decrementAndGet();

            Timer publicationTimer = meterRegistry.timer("event_publication_duration",
                    Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode));
            publicationTimer.record(durationInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventPublicationFailed", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventPublicationFailedForMirroring(InflightEvent event, Exception exception) {
        putInfoInMDC(event, "PUBLICATION_FAILED_MIRRORING", null, null);
        String msg = "";
        try {
            msg = String.format("Event publication failed for mirroring. Exception is `%s`. Event is %s.",
                    (exception != null ? exception.getMessage() : ""),  event.toShortLog());
            LOGGER.error(msg, exception);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventPublicationFailedForMirroring", ex);
        }
        try {
            String publicationCode = event.getPublicationCode();
            String eventTypeCode = event.getEventTypeCode();

            Counter counter1 = meterRegistry.counter("event_publications_failed_for_mirroring_total",
                    Tags.of("publication_code", publicationCode, "event_type_code", eventTypeCode));
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventPublicationFailedForMirroring", ex);
        }
        removeInfoInMDC();
        return msg;
    }


    // DELIVERY ////////////////////////////////////////////////////////////////////////////////////////////////////////


    public synchronized String eventDeliveryRequested(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_REQUESTED", null, null);
        String msg = "";
        try {
            msg = String.format("Event delivery requested. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryRequested", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_requested_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryRequested", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToExpiredEvent(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_EXPIRED_EVENT", null, null);
        String msg = "";
        try {
            msg = String.format("Event delivery aborted due to expired event. Event is %s.", event.toShortLog());
            LOGGER.warn(msg);   // WARN and not ERROR !
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToExpiredEvent", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_expired_event_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToExpiredEvent", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToInactiveEventProcessing(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_INACTIVE_EVENT_PROCESSING", null, null);
        String msg = "";
        try {
            msg = String.format("Event delivery aborted due to inactive event processing. Event is %s.", event.toShortLog());
            LOGGER.warn(msg);   // WARN and not ERROR !
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToInactiveEventProcessing", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_inactive_event_processing_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToInactiveEventProcessing", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToInvalidSubscriptionCode(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_INVALID_SUBSCRIPTION_CODE", null, null);
        String msg = "";
        try {
            String subscriptionCode = event.getSubscriptionCode();
            msg = String.format("Event delivery aborted due to invalid subscription code '%s'. Event is %s.", subscriptionCode, event.toShortLog());
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToInvalidSubscriptionCode", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_invalid_subscription_code_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToInvalidSubscriptionCode", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToInactiveSubscription(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_INACTIVE_SUBSCRIPTION", null, null);
        String msg = "";
        try {
            String subscriptionCode = event.getSubscriptionCode();
            msg = String.format("Event delivery aborted due to inactive subscription '%s'. Event is %s.", subscriptionCode, event.toShortLog());
            LOGGER.warn(msg);   // WARN and not ERROR !
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToInactiveSubscription", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_inactive_subscription_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToInactiveSubscription", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToInvalidEventTypeCode(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_INVALID_EVENT_TYPE_CODE", null, null);
        String msg = "";
        try {
            String eventTypeCode = event.getEventTypeCode();
            msg = String.format("Event delivery aborted due to invalid event type code '%s'. Event is %s.", eventTypeCode, event.toShortLog());
            LOGGER.error(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToInvalidEventTypeCode", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_invalid_event_type_code_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToInvalidEventTypeCode", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToInactiveEventType(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_INACTIVE_EVENT_TYPE", null, null);
        String msg = "";
        try {
            String eventTypeCode = event.getEventTypeCode();
            msg = String.format("Event delivery aborted due to inactive event type '%s'. Event is %s.", eventTypeCode, event.toShortLog());
            LOGGER.warn(msg);   // WARN and not ERROR !
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToInactiveEventType", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_inactive_event_type_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToInactiveEventType", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAbortedDueToNotMatchingChannel(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ABORTED_NOT_MATCHING_CHANNEL", null, null);
        String msg = "";
        try {
            String channel = event.getChannel();
            msg = String.format("Event delivery aborted due to not matching channel '%s'. Event is %s.", channel, event.toShortLog());
            LOGGER.warn(msg);   // WARN and not ERROR !
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAbortedDueToNotMatchingChannel", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_aborted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
            Counter counter2 = meterRegistry.counter("event_deliveries_aborted_due_to_not_matching_channel_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter2.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metrics for eventDeliveryAbortedDueToNotMatchingChannel", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryAttempted(InflightEvent event) {
        putInfoInMDC(event, "DELIVERY_ATTEMPTED", null, null);
        String msg = "";
        try {
            msg = String.format("Event delivery attempted. Event is %s.", event);
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryAttempted", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();
            Counter counter1 = meterRegistry.counter("event_deliveries_attempted_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventDeliveryAttempted", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliverySucceeded(InflightEvent event, Instant deliveryStart) {
        Instant deliveryEnd = Instant.now();
        Long durationInMillis = deliveryStart != null ? Duration.between(deliveryStart, deliveryEnd).toMillis() : null;
        putInfoInMDC(event, "DELIVERY_SUCCEEDED", durationInMillis, TimeUnit.MILLISECONDS);
        String msg = "";
        try {
            msg = String.format("Event delivery succeeded. Event is %s.", event.toShortLog());
            LOGGER.debug(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliverySucceeded", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();

            Counter counter1 = meterRegistry.counter("event_deliveries_succeeded_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();

            Timer deliveryTimer = meterRegistry.timer("event_delivery_duration",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            deliveryTimer.record(durationInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventDeliverySucceeded", ex);
        }
        removeInfoInMDC();
        return msg;
    }

    public synchronized String eventDeliveryFailed(InflightEvent event, Exception exception, Instant deliveryStart) {
        Instant deliveryEnd = Instant.now();
        Long durationInMillis = deliveryStart != null ? Duration.between(deliveryStart, deliveryEnd).toMillis() : null;
        putInfoInMDC(event, "DELIVERY_FAILED", durationInMillis, TimeUnit.MILLISECONDS);
        String msg = "";
        try {
            msg = String.format("Event delivery failed. Exception is `%s`. Event is %s.",
                    (exception != null ? exception.getMessage() : ""),  event.toShortLog());
            LOGGER.error(msg, exception);
        } catch (Exception ex) {
            LOGGER.error("Error while recording log for eventDeliveryFailed", ex);
        }
        try {
            String subscriptionCode = event.getSubscriptionCode();
            String eventTypeCode = event.getEventTypeCode();
            String publicationCode = event.getPublicationCode();

            Counter counter1 = meterRegistry.counter("event_deliveries_failed_total",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            counter1.increment();

            Timer deliveryTimer = meterRegistry.timer("event_delivery_duration",
                    Tags.of("subscription_code", subscriptionCode, "event_type_code", eventTypeCode, "publication_code", publicationCode));
            deliveryTimer.record(durationInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("Error while recording metric for eventDeliveryFailed", ex);
        }
        removeInfoInMDC();
        return msg;
    }
}
