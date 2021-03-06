# simple-event-broker-rmq project

This version uses SpringBoot and RabbitMQ.


## The various modules/components
- RabbitMQ. In dev mode, it uses port 5672 (for publish & subscribe) and port 15672 (for the RabbitMQ console).
- Eureka Service Discovery. Deactivated for the moment. It uses port 8761 (default port for Eureka service).
- Publication Gateway
- Publication Adapter
  Publication Manager
- Subscription Manager
- Subscription Adapter
- Catalog
- Catalog Adapter
- Test/Fake Subscriber
- Probe
- OperationManager
- OperationAdapter

A `Manager` (`PublicationManager` and `SubscriptionManager`) contains the logic of publication/subscription, the management
of acknowledgment of message/event (either positive or negative acknowledgment), the interaction with the underlying 
broker system (here RabbitMQ).

An `Adapter` (`PublicationAdapter` and `SubscriptionAdapter`) contains dummy/boilerplate code to adapt the interactions 
between the broker and its ecosystem which may vary a lot. For example, in some contexts, it is ok to secure the call to 
the webhooks with just BasicAuth. In other contexts, OAuth2 may be required. Or some other security mechanism specialized
for an organization. The Adapters are independent of the underlying broker technology (RabbitMQ, Kafka, Pulsar...).

The roles and responsibilities of managers and adapters are clearly defined to allow the seamless replacement of a
component by another.

The `Catalog` is in charge of the management of the objects `EventType`, `Publication` and `Subscription`.
The `CatalogAdapter` is an adapter between the `Catalog` component and the other components that needs access to the 
Catalog to operate.
 
The `Publication Gateway` is the entry point to publish an event. It is based on a `Spring Cloud Gateway`. It offers 
automatic retry feature to ensure no interruption service even in the case of the release of a new version of 
a `PublicationAdapter`.


## Ports

Ports used by the various components:
- `8080`: port for the `PublicationGateway`
- `8081`: port for the `PublicationAdapterRI`
- `8082`: port for the `PublicationManager`
- `8083`: port for the `SubscriptionManager`
- `8084`: port for the `SubscriptionAdapterRI`
- `8085`: port for the `CatalogAdapterRI`
- `8086`: port for the `Catalog`
- `8087`: port for the `OperationAdapterRI`
- `8088`: port for the `OperationManager`
- `8089`: port for the `Probe`
- `8090`: port for the `TestServer`
- `8099`: port for the `TestSubscriber` and `TestSubscriberOauth2`


## Event flow

1. Event Publisher 
2. Publication Gateway 
3. Publication Adapter 
4. Publication Manager
5. RabbitMQ
6. Subscription Manager
7. Subscription Gateway (optional)
8. Subscription Adapter
9. Event Subscriber


## Event expiration

An event will expire if one of the following condition is met:
- the `timeToLiveInSeconds` of the published event has been reached (attribute timeToLiveInSeconds of the event sent to the 
`/events` endpoint exposed by the Publication Gateway or the Publication Adapter)
- the webhook returned a 401 (UNAUTHORIZED) or 403 (FORBIDDEN) HTTP status code and the 
`broker.default-time-to-live-in-seconds-for-webhook-auth401Or403-error` (set in the SubscriptionManager config) has been reached.
- the webhook returned a 4xx (but not 401 nor 403) HTTP status code and the 
`broker.default-time-to-live-in-seconds-for-webhook-client4xx-error` (set in the SubscriptionManager config) has been reached.
- the webhook returned a 5xx HTTP status code and the `broker.default-time-to-live-in-seconds-for-webhook-sever5xx-error` 
(set in the SubscriptionManager config) has been reached.
- the webhook was unreachable (due to a wrong URL of the webhook, a network route not open...) and the 
`broker.default-time-to-live-in-seconds-for-webhook-connection-error` (set in
the SubscriptionManager config) has been reached.
- the webhook was reached but never returned a response before the `webhookReadTimeoutInSeconds` timeout (set in the 
SubscriptionAdapter and the SubscriptionManager config) was triggered and the 
`broker.default-time-to-live-in-seconds-for-webhook-read-timeout-error` (set in the SubscriptionManager config) has been reached.

Those `broker.default-time-to-live-in-seconds-for-webhook-XXX-error` default values can be overridden for a given 
subscription using the attributes `timeToLiveInSecondsForWebhookXXXError` of the Subscription object (in the `common` 
module).

When an event expires, it is sent to a `DeadLetterQueue (DLQ)` for analysis of the cause of a failed delivery of the 
event to a subscriber. The DeadLetterQueue uses the queue name `DLQ_{subscriptionCode}`.
 
 
## Predefined Event Types samples

The `Catalog` module contains some predefined event types samples. Those predefined event types (and the related
publications and subscriptions) are declared in the `resources/import.sql` file of the `Catalog` module. 

Those predefined event types samples are:
- NominalTest-EVT
- Failure401Test-EVT
- Failure500Test-EVT
- SlowTest-EVT
- ComplexPayloadTest-EVT
- ComplexPayload2Test-EVT
- TimeToLiveTest-EVT
- OAuth2Test-EVT

For each predefined event types samples, there is a webhook endpoint exposed by the `TestSubscriber1Controller` class 
in the `test-subscriber` module (exception for the `OAuth2Test-EVT` Event Type which uses an endpoint exposed by the 
`TestSubscriber1Controller` class in the `test-subscriber-oauth2` module).

To publish an event for one of those predefined event types samples named `XxxxxTest-EVT`, use the publication code
`XxxxxTest-PUB` when publishing the event on the endpoint `/events`.


## Error management (with HTTP status codes)

The `Publication Adapter` returns the following HTTP status codes:
- `201 CREATED` if the publication succeeded (the `Publication Manager` returned a `2xx success` code)
- The status code returned by the `Publication Manager` if it returned a `4xx client error` or a `5xx server error` code
- `502 BAD GATEWAY` if the connection to the `Publication Manager` failed (you can use the 
`broker.connect-timeout-in-seconds-for-publication-manager` property to set an appropriate timeout)
- `504 GATEWAY TIMEOUT` if the`Publication Manager` did not respond within the allotted time (you can use the 
`broker.read-timeout-in-seconds-for-publication-manager` property to set an appropriate timeout)
- `500 INTERNAL SERVER ERROR` for unexpected error
 
The `Publication Manager` returns the following HTTP status codes:
- `201 CREATED` if the publication succeeded (the event was sent to RabbitMQsuccessfully)
- `400 BAD REQUEST` if the publication code is missing in the published event
- `400 BAD REQUEST` if the publication code is invalid (no Publication found with this code)
- `400 BAD REQUEST` if the publication is inactive
- `500 INTERNAL SERVER ERROR` if no Event Type is associated with this Publication
- `500 INTERNAL SERVER ERROR` if the creation of a RabbitMQ producer failed
- `500 INTERNAL SERVER ERROR` if the sending of the event to RabbitMQ failed
- `500 INTERNAL SERVER ERROR` for unexpected error

The `Subscription Adapter` returns the following HTTP status codes:
- `200 OK` and `InflightEvent.webhookHttpStatus` set with the status code returned by the webhook if the call to the webhook succeeded (the webhook returned a `2xx success`code)
- `200 OK` and `InflightEvent.webhookHttpStatus` set with the status code returned by the webhook if it returned a `4xx client error` or a `5xx server error` code
- `200 OK` and `InflightEvent.webhookHttpStatus` set with the status code `502 BAD GATEWAY` if the connection to the webhook failed
(you can use the `broker.connect-timeout-in-seconds-for-webhooks` property to set an appropriate timeout)
- `200 OK` and `InflightEvent.webhookHttpStatus` set with the status code `504 GATEWAY TIMEOUT` if webhook did not respond within the allotted time 
(you can use the `broker.read-timeout-in-seconds-for-webhooks` property to set an appropriate timeout)
- `401 UNAUTHORIZED` if the OAuth2 token could not be delivered (bad credentials, bad scope, access to the OAuth2 Authorization Server failed... )   
- `500 INTERNAL SERVER ERROR` if credentials are missing (for BasicAuth) or scope is missing (for OAuth2)
- `500 INTERNAL SERVER ERROR` for unexpected error

In the `Subscription Manager`, a message is acknowledged if:
- the event has expired
- the Subscription is inactive
- The Event Type is inactive
- The channel of the event and the channel of the subscription do not match
- the `Subscription Adapter` returned `200 OK` with `InflightEvent.webhookHttpStatus` set with a `2xx success` code

In the `Subscription Manager`, a message is *negatively* acknowledged (so it will be redelivered) if:
- the `Subscription Adapter` returned a connection error or a read timeout error or a 4xx client error or a 5xx server error
when calling the webhook (the error when calling the webhook is reported using the `InflightEvent.webhookHttpStatus` attribute, 
NOT with the HTTP status code returned by the `Subscription Adapter`)
- the connection to the `Subscription Adapter` failed 
(you can use the `broker.connect-timeout-in-seconds-for-subscription-adapter` property to set an appropriate timeout)
- the `Subscription Adapter` did not respond within the allotted time (you can use the 
`broker.read-timeout-in-seconds-for-subscription-adapter` property to set an appropriate timeout)
- the `Subscription Adapter` returned a `4xx client error` or a `5xx server error` code
- an unexpected error occurred


## Liveness and readiness probes

Liveness and readiness probes have been introduced in Spring Boot 2.3.
See https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot

Pay attention to the difference between liveness and readiness. In case of a liveness failure, Kubernetes will kill the 
pod and restart another one. In case of a readiness failure, Kubernetes will stop to route the traffic to the pod until
the readiness state succeed. Liveness state should be used only for non-recoverable errors.

In this project, liveness and readiness probes are used as follows:
- The Catalog, CatalogAdapter, Publication Manager and Publication Adapter modules expose their probes at `/actuator/health/liveness` and 
`/actuator/health/readiness`
- Those endpoints return `200 OK` if the state is healthy and `503 Service Unavailable` if the state is unhealthy
- The Publication Adapter is ready if the Publication Manager is ready
- The Publication Manager is ready if the Catalog Adapter is ready
- The Catalog Adapter is ready if the Catalog is ready
- The Catalog is ready if its database is ready (if it can load event types from the db without error)
 

## Run the modules/components

Start the modules in the order below.

### Run RabbitMQ
```
./start-rabbitmq.sh
```

### Run the Eureka Service Discovery
```
cd eureka-service
../mvnw clean spring-boot:run
```
>You should start only one instance of the Eureka Service Discovery

### Compile and install the Commons module
```
cd commons
../mvnw clean install
```

### Run the Catalog
```
cd catalog
../mvnw clean spring-boot:run
```
>You should start only one instance of the Catalog (because this simple project uses an in-memory database H2)

### Run the Catalog Adapter
```
cd catalog-adapter-ri
../mvnw clean spring-boot:run
```
>You should start only one instance of the Catalog Adapter (you could start many instances, but 1 instance is sufficient)

### Run the Subscription Adapter

The configuration of the `Subscription Adapter` depends on whether OAuth2 is used to secure your webhooks (or whether
simple BasicAuth is used or no authentication).

>You can start multiple instances of the Subscription Adapter

### If you webhooks are secured using OAuth2 ###

The Subscription Adapter requires to get OAuth2 Access Tokens from an AuthorizationServer if some webhooks are secured 
using OAuth2. In that case, the Subscription Adapter must be declared in the AuthorizationServer to get its clientId
and clientSecret to authenticate itself. 

For this project, I used a free developer account on Okta (https://developer.okta.com).

To create a `simple-event-broker-rmq` service/application in Okta, select the Applications/Add Application menu, then choose an
application of type `Service`, then fill in the name `simple-event-broker-rmq`. Once the application is created, copy/paste
the clientId and clientSecret (they will be copied in the `set-credentials.sh` file below).

To add a OAuth2 scope for your webhooks, select the API/Authorization Servers menu, then choose the `default` Authorization Server,
then select the Scopes tab, then add scope `test_subscriber_oauth2.webhooks`.

Create a file `set-credentials.sh` in the `subscription-adapter` directory with the following lines:
```
export OAUTH2_CLIENT_ID=<PUT_HERE_YOUR_OWN_OAUTH2_CLIENT_ID>
export OAUTH2_CLIENT_SECRET=<PUT_HERE_YOUR_OWN_OAUTH2_CLIENT_SCERET>
```
Of course, a file with such sensitive data is not committed in the source code repository so you have to create your own
version locally.

Update the `src/main/resources/application.properties` file to set the `broker.oauth2-token-endpoint` property with
the URL of the default Authorization Server of your Okta account (for example: `https://dev-123456.okta.com/oauth2/default/v1/token`; 
replace `123456` with your actual organization id in Okta).

Once the `set-credentials.sh`file contains the right credentials and the `application.properties` has been updated, 
then run the Subscription Adapter:
```
cd subscription-adapter-ri
source set-credentials.sh
../mvnw clean spring-boot:run
```

### If your webhooks are not secured or secured using BasicAuth ###

In that case, there is no need for the Subscription Adapter to get OAuth2 Access Tokens from an AuthorizationServer.

So you can simply run the Subscription Adapter (without `set-credentials.sh`file):
```
cd subscription-adapter-ri
../mvnw clean spring-boot:run
```

### Run the Subscription Manager

>You can start multiple instances of the Subscription Manager but if you start multiple instances, you must pay
>attention to provide the right configuration on the start command line (to override the default properties 
>`broker.cluster-size` and `broker.cluster-node-index`).
>Cluster size is the number of SubscriptionManager instances and Cluster index is the index of this
>SubscriptionManager instance within the cluster.
>Cluster index must  be ***UNIQUE*** within the cluster and must follow the sequence 0, 1... < Cluster size.
>The SubscriptionManager instance in charge of the management of an event is the instance that meets the
>criteria `broker.cluster-node-index == eventTypeCode.hashCode() % broker.cluster-size`.
>For a given event type, only one instance of SubscriptionManager will manage the events.

>**Warning**: This special configuration when multiple instances ofSubscription Manager are used is mandatory to 
>guarantee the delivery order of the events (otherwise, 2 instances could process at different pace the events with a same 
>EventTypeCode pace, which would mess the delivery order).


#### With one instance only of Subscription Manager
```
cd subscription-manager
../mvnw clean spring-boot:run
```
#### With 2 instances of Subscription Manager
Start the instance #1 with:
```
cd subscription-manager
../mvnw clean spring-boot:run -Dspring-boot.run.arguments="--broker.cluster-size=2 , --broker.cluster-node-index=0"
```
Start the instance #2 with:
```
cd subscription-manager
../mvnw clean spring-boot:run -Dspring-boot.run.arguments="--broker.cluster-size=2 , --broker.cluster-node-index=1"
```

### Run the Publication Manager
```
cd publication-manager
../mvnw clean spring-boot:run
```
>You can start multiple instances of the Publication Manager

### Run the Publication Adapter
```
cd publication-adapter-ri
../mvnw clean spring-boot:run
```
>You can start multiple instances of the Publication Adapter

### Run the Publication Gateway
```
cd publication-gateway
../mvnw clean spring-boot:run
```
>You should start only one instance of the Publication Gateway


### Run the Test/fake Subscriber
```
cd test-subscriber
../mvnw clean spring-boot:run
```
>You should start only one instance of the Test/Fake Subscriber

### Run the Operation Manager (optional)
```
cd operation-manager
../mvnw clean spring-boot:run
```
>You should start only one instance of the Operation Manager

### Run the Operation Adapter (optional)
```
cd operation-adapter-ri
../mvnw clean spring-boot:run
```
>You should start only one instance of the Operation Adapter



## Test

### Nominal test
In this scenario, there are 2 up & healthy subscriptions, so for each published event, there are 2 successful deliveries.

```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "NominalTest-PUB","payload": { "message": "NominalTest" }, "timeToLiveInSeconds": 30 }' \
  http://localhost:8081/events
```

### Test with a faulty (HTTP status code 500) subscription
In this scenario, there are 1 up & healthy subscription but 1 up & faulty subscription which returns a HTTP status code 500 (interval server error), 
so for each published event, there are 1 successful delivery and 1 failed delivery.
```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "Failure500Test-PUB","payload": { "message": "Failure500Test" }, "timeToLiveInSeconds": 30 }' \
  http://localhost:8081/events
```

### Test with a slow subscription (2s to process the event)
In this scenario, there are 1 up & healthy subscription but 1 up & slow subscription which processes the event in 2 seconds, 
so for each published event, there are 2 successful deliveries, but one delivery is slow.

If necessary, use the `broker.read-timeout-in-seconds-for-webhooks` property (default is 10) to set an appropriate timeout.
```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "SlowTest-PUB","payload": { "message": "SlowTest", "timeToSleepInMillis": 2000 }}' \
  http://localhost:8081/events
```

### Test with a too slow subscription (60s to process the event so a timeout is triggered)
In this scenario, there are 1 up & healthy subscription but 1 up & too slow subscription which processes the event in 60 seconds, 
so for each published event, there are 1 successful delivery and 1 failed delivery (failed because of the timeout).

If necessary, use the `broker.read-timeout-in-seconds-for-webhooks` property (default is 10) to set an appropriate timeout.
```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "SlowTest-PUB","payload": { "message": "SlowTest", "timeToSleepInMillis": 60000 }}' \
  http://localhost:8081/events
```

### Test with a complex payload 
```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "ComplexPayloadTest-PUB","payload": { "message": "ComplexPayloadTest", "timeToSleepInMillis": 2000, "items": [{ "param1": "hello", "param2": "world" }]}, "timeToLiveInSeconds": 30 }' \
  http://localhost:8081/events
```
```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "ComplexPayload2Test-PUB","payload": { "message": "ComplexPayload2Test", "timeToSleepInMillis": 2000, "someStringParam": "hello world", "items2": [{ "p1": 1, "p2": 2 }]}, "timeToLiveInSeconds": 30 }' \
  http://localhost:8081/events
```

### Test with subscriber webhooks secured using OAuth2 

```
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"publicationCode": "OAuth2Test-PUB","payload": { "message": "NominalTest with OAuth2" }, "timeToLiveInSeconds": 30 }' \
  http://localhost:8081/events
```



## TestServer

The `TestServer` module contains synthetic tests (tests that simulate actual publishers and subscribers).

Instead of running tests from standalone tools/utilities/scripts, tests have been grouped in a microservice.
Those tests can be triggered via a web browser (for manual tests) or from a `curl` command for automatic tests (
automatic tests are typically grouped in one or several shell script(s) that is(are) executed at regular interval using 
some `cron` utility).

### Commands

The `TestServer` module accepts the following commands:
- `pub/run`: executes the test. Execution can be synchronous or asynchronous (for long running tests). You can specify how 
  many events to publish and the pause (in millis) between each published event. 
- `pub/stop`: stops a given test or the last started test
- `pub/suspend`: suspends a given test or the last started test. Use the `resume` command to resume the test.
- `pub/resume`: resumes a given test or the last started test
- `pub/slowdown`: slows down the publications for a given test or the last started test
- `sub/accept`: the webhook will return the provided status code or the default successful status code
- `sub/reject`: the webhook will return the provided status code or the default failure status code
- `sub/slowdown`: the webhook will slow down to simulate slow consumers.
  
### Test Event Payload

The Test Event Payload contains the following attributes:
- `String testId`: id of the test
- `Instant testTimestamp`: timestamp of the test (start of the test)
- `String message`: any message (e.g. "Hello World")
- `Instant eventTimestamp`: timestamp of the current event of the test
- `long index`: index of the current event (to check delivery order)
_ `long pauseInMillisForWebhook`: pause in millis to do on the webhook side (to simulate slow consumers) 
- `boolean isFirstEvent`: true if this event is the first event of the test
- `boolean isLastEvent`: true if this event is the last event of the test
- `Long expectedCount`: expected count of events. *Only present in the last event of the test*.
  

### Available Tests

The `TestServer` module contains the following tests:
- `nominal`: checks that all events are delivered and in the correct order. This test uses a webhook at `/tests/nominal/webhook`.
  

### Examples
Examples (with the `nominal` test):
```
curl http://localhost:8090/tests/nominal/pub/run
curl "http://localhost:8090/tests/nominal/pub/run?publicationCode=TestPub&timeToLiveInSeconds=60&channel=mychannel&n=100&pause=1000&pauseForWebhook=1000&sync=true"    
curl http://localhost:8090/tests/nominal/pub/stop
curl "http://localhost:8090/tests/nominal/pub/stop?testId=123456789"
curl http://localhost:8090/tests/nominal/pub/slowdown
curl "http://localhost:8090/tests/nominal/pub/slowdown?testId=123456789&pause=10000"
curl http://localhost:8090/tests/nominal/sub/accept
curl "http://localhost:8090/tests/nominal/sub/accept?testId=123456789&status=201"
curl http://localhost:8090/tests/nominal/sub/reject
curl "http://localhost:8090/tests/nominal/sub/reject?testId=123456789&status=500"
curl http://localhost:8090/tests/nominal/sub/slowdown
curl "http://localhost:8090/tests/nominal/sub/slowdown?testId=123456789&pause=10000"
```

```
curl "http://localhost:8090/tests/nominal/pub/run?n=10&pause=1000&timeToLiveInSeconds=30"
curl http://localhost:8090/tests/nominal/sub/reject
curl http://localhost:8090/tests/nominal/sub/accept
curl http://localhost:8090/tests/nominal/pub/stop
```


### Recordings

The `TestServer` module records the results of the tests (useful to compare performance results before and after an 
optimization or to detect performance degradation over time).

To view those test records, go to `http://localhost:8090/tests/recordings` (the last test result is displayed at the top of the list).

To view records for *failed* tests only, go to `http://localhost:8090/tests/recordings?status=failed`

A Test Record contains the following attributes:
- `String testId`
- `Instant testTimestamp`
- `boolean finished`
- `boolean succeeded`
- `String reasonIfFailed`
- `Long durationInSeconds`
- `Long averageRoundtripDurationInMillis`
- `Long expectedCount`
- `Long actualCount`
- `String hostName`

>Planned enhancements: filters with `from` and `to` (time range)...


### Logs

For standard logs, use the Spring profile `stdlogs`.

For JSON logs, use the Spring profile `jsonlogs`.

> To set the Spring profile, either set the `spring.profiles.active` property in the `application.properties` config file or 
> set the command line argument `-Dspring.profiles.active`.

For JSON logs (in files), there is a rotation every day or if the size of the file exceeds 10MB.

For JSON logs, the `logstash-logback-encoder` is used (https://github.com/logstash/logstash-logback-encoder).

The `logstash-logback-encoder` generates the following JSON attributes for the log:
- `@timestamp`: Time of the log event (yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
- `@version`: Logstash format version (e.g. 1)
- `message`: Formatted log message of the event
- `logger_name`: Name of the logger that logged the event
- `thread_name`: Name of the thread that logged the event
- `level`: String name of the level of the event
- `level_value`: Integer value of the level of the event
- `stack_trace`: (Only if a throwable was logged) The stacktrace of the throwable. Stackframes are separated by line endings.
- `tags`: (Only if tags are found)

Logs may contain the following information if relevant for the log:
- `event_type_code`
- `publication_code`
- `subscription_code`
- `message_code`
- `duration_in_sec`
- `duration_in_millis`

The following `message_code` are used:
- `PUBLICATION_REQUESTED`
- `PUBLICATION_REJECTED_MISSING_PUBLICATION_CODE`
- `PUBLICATION_REJECTED_INVALID_PUBLICATION_CODE`
- `PUBLICATION_REJECTED_INACTIVE_PUBLICATION`
- `PUBLICATION_REJECTED_INVALID_AUTH_CLIENT_ID`
- `PUBLICATION_REJECTED_INVALID_EVENT_TYPE_CODE`
- `PUBLICATION_REJECTED_INACTIVE_EVENT_TYPE`
- `PUBLICATION_ATTEMPTED`
- `PUBLICATION_SUCCEEDED` (the `duration_in_millis` info is provided)
- `PUBLICATION_FAILED` (the `duration_in_millis` info is provided)
- `PUBLICATION_FAILED_MIRRORING`
- `DELIVERY_REQUESTED`
- `DELIVERY_ABORTED_EXPIRED_EVENT`
- `DELIVERY_ABORTED_INACTIVE_EVENT_PROCESSING`
- `DELIVERY_ABORTED_INVALID_SUBSCRIPTION_CODE`
- `DELIVERY_ABORTED_INACTIVE_SUBSCRIPTION`
- `DELIVERY_ABORTED_INVALID_EVENT_TYPE_CODE`
- `DELIVERY_ABORTED_INACTIVE_EVENT_TYPE`
- `DELIVERY_ABORTED_NOT_MATCHING_CHANNEL`
- `DELIVERY_ATTEMPTED`
- `DELIVERY_SUCCEEDED` (the `duration_in_millis` info is provided)
- `DELIVERY_FAILED` (the `duration_in_millis` info is provided)
  


### Metrics

All metrics are produced using the Prometheus format and are exposed at the `/actuator/prometheus` endpoint.

The following tags are present for all metrics:
- `component_name` (as defined in th `application.properties` config file)
- `component_instance_id` (as defined in th `application.properties` config file)
- `host_name`

The following metrics are produced:
- Counter `event_publications_requested_total`
- Counter `event_publications_rejected_total`
- Counter `event_publications_rejected_due_to_missing_publication_code_total`
- Counter `event_publications_rejected_due_to_invalid_publication_code_total`
- Counter `event_publications_rejected_due_to_inactive_publication_total`
- Counter `event_publications_rejected_due_to_invalid_auth_client_id_total`
- Counter `event_publications_rejected_due_to_invalid_event_type_code_total`
- Counter `event_publications_rejected_due_to_inactive_event_type_total`
- Counter `event_publications_attempted_total`
- Counter `event_publications_succeeded_total`
- Counter `event_publications_failed_total`
- Counter `event_publications_failed_for_mirroring_total`
- Gauge `pending_event_publications`
- Timer `event_publication_duration`
- Counter `event_deliveries_requested_total`
- Counter `event_deliveries_aborted_total`
- Counter `event_deliveries_aborted_due_to_expired_event_total`
- Counter `event_deliveries_aborted_due_to_inactive_event_processing_total`
- Counter `event_deliveries_aborted_due_to_invalid_subscription_code_total`
- Counter `event_deliveries_aborted_due_to_inactive_subscription_total`
- Counter `event_deliveries_aborted_due_to_invalid_event_type_code_total`
- Counter `event_deliveries_aborted_due_to_inactive_event_type_total`
- Counter `event_deliveries_aborted_due_to_not_matching_channel_total`
- Counter `event_deliveries_attempted_total`
- Counter `event_deliveries_succeeded_total`
- Counter `event_deliveries_failed_total`
- Timer `event_delivery_duration`


#### Metrics for TestServer
The `TestServer` module generates the following metrics (at the `/actuator/prometheus` endpoint):
- `test_started_total`
- `test_succeeded_total`
- `test_failed_total`
- `test_stopped_total`
- `test_event_publication_attempted_total`
- `test_event_publication_succeeded_total`
- `test_event_publication_failed_total`
- `test_event_received_total`
- `test_event_roundtrip_duration`
- `test_event_missing_or_unordered_total`
- `test_event_counts_mismatch_total`



## Operation Manager

The `OperationManager` offers various operations for support and maintenance on the broker (such as
get the event blocked in a queue, delete the event blocked in a queue, delete all events in a queue... ).


### Commands:
The `OperationManager` module accepts the following commands:
- `GET /subscriptions/{subscriptionCode}/events/next`: get the next (=blocked) event in the queue for a given subscription.
- `DELETE /subscriptions/{subscriptionCode}/events/next`: delete the next (=blocked) event in the queue for a given subscription.
- `DELETE /subscriptions/{subscriptionCode}/events`: delete all events in the queue for a given subscription.
- `GET /subscriptions/{subscriptionCode}/dead-letter-queue/events/next`: get the next event in the dead letter queue for a given subscription.
- `DELETE /subscriptions/{subscriptionCode}/dead-letter-queue/events/next`: delete the next event in the dead letter queue for a given subscription.
- `DELETE /subscriptions/{subscriptionCode}/dead-letter-queue/events`: delete all events in the dead letter queue for a given subscription.
- `GET /subscriptions/{subscriptionCode}/rabbitmq/queue/info`: get info on the RabbitMQ queue for a given subscription.
- `GET /rabbitmq/overview`: get an overview of the RabbitMQ broker.
- `POST /event-processing/activate`: activate the event processing (in the `SubscriptionManager`)
- `POST /event-processing/deactivate`: deactivate the event processing (in the `SubscriptionManager`)


### Examples
Examples (with the `nominal` test):
```
curl http://localhost:8088/subscriptions/TestServer-Nominal-SUB/events/next
curl --request DELETE http://localhost:8088/subscriptions/TestServer-Nominal-SUB/events/next
curl --request DELETE http://localhost:8088/subscriptions/TestServer-Nominal-SUB/events
curl http://localhost:8088/subscriptions/TestServer-Nominal-SUB/dead-letter-queue/events/next
curl --request DELETE 8088://localhost:43067/subscriptions/TestServer-Nominal-SUB/dead-letter-queue/events/next
curl --request DELETE 8088://localhost:43067/subscriptions/TestServer-Nominal-SUB/dead-letter-queue/events
```

```
curl http://localhost:8088/subscriptions/TestServer-Nominal-SUB/rabbitmq/queue/info
curl http://localhost:8088/rabbitmq/overview
curl --request POST http://localhost:8088/event-processing/activate
curl --request POST http://localhost:8088/event-processing/deactivate
```



## Operation Adapter

The `OperationAdapter` offers the same operations than the `OperationManager` but returns events of the class 
`EventToSubscriber` instead of events of the class `InflighEvent` (only Managers can handle `InflighEvent`, not Adapters).

### Examples
See the examples of the `OperationAdapter` (replace only the server host and server port).



## Misc

### kill a process that runs on a given port

```
kill $(lsof -t -i:<port>)
```

### BCrypt encryption for credentials

Some credentials (in the `application.properties` files) are encrypted using BCrypt. To encrypt some password, you can
use an online BCrypt Hash Generator and Checker, such as https://www.devglan.com/online-tools/bcrypt-hash-generator


## RabbitMQ Console

RabbitMQ console: http://localhost:15672/
Default login and password are "guest".



## Quick start in local


### Download this repository

`git clone` or download this repository as zip file.


### RabbitMQ config

You can start a RabbitMQ server locally using the `start-rabbitmq.sh` script file but it requires that Docker is installed on 
the machine.

If you already have a RabbitMQ server for your homologation environnement, then you will have to update the following 
properties files to set the URL and the login/password to access RabbitMQ:
- `publication-manager/src/main/resources/application-homol.properties`
- `subscription-manager/src/main/resources/application-homol.properties`
- `operation-manager/src/main/resources/application-homol.properties`

Set the following properties in the config files: 
```
# RabbitMQ
broker.rabbitmq-host = PUT_HERE_THE_HOST_FOR_HOMOL
# 5671 is with SSL, 5672 withOUT SSL
broker.rabbitmq-port = 5671
broker.rabbitmq-username = PUT_HERE_THE_USERNAME_FOR_HOMOL
broker.rabbitmq-password = PUT_HERE_THE_PASSWORD_FOR_HOMOL
broker.rabbitmq-ssl-enabled = true
```


### Elastic config for logs

If you want to have logs not only in the console and in rolling files, but also in ElasticSearch, then you will have to 
update the following properties files to set the URL and the login/password to access ElasticSearch:
- `publication-manager/src/main/resources/logback-spring-elastic.properties`
- `subscription-manager/src/main/resources/logback-spring-elastic.properties`
>As of now, Elastic logs are only available for PublicationManager and Subscription Manager.

Set the following property in the config files:
```
<property scope="context" name="MY_ELASTICSEARCH_URL" value="http://elastic:changeme@localhost:9200" />
```
>In `http://elastic:changeme@localhost:9200` URL, `elastic` is the username and `changeme` is the password.

To start a PublicationManager with Elastic logs, run: `java -jar -Dspring.profiles.active=homol -Dlogging.config=classpath:logback-spring-elastic.xml target/publication-manager-1.0-SNAPSHOT.jar`

To start a SubscriptionManager with Elastic logs, run: `java -jar -Dspring.profiles.active=homol -Dlogging.config=classpath:logback-spring-elastic.xml target/subscription-manager-1.0-SNAPSHOT.jar`



### Run the various components

>If you want to set a profile according to your environment (for example `local` or `homol` or `prod`), 
> use `../mvnw spring-boot:run -Dspring-boot.run.profiles=<profile>`
> 
>`local` is the default profile if no profile is provided.
> 
>
> Examples:
> 
>`../mvnw spring-boot:run -Dspring-boot.run.profiles=local`
>
>`../mvnw spring-boot:run -Dspring-boot.run.profiles=homol`
>
>`../mvnw spring-boot:run -Dspring-boot.run.profiles=prod`

>If you prefer to use the `java` command instead of the `mvnw` command, use
> `java -jar -Dspring.profiles.active=<profile> XXX.jar`
>
> Examples:
>
>`java -jar -Dspring.profiles.active=local target/publication-manager-1.0-SNAPSHOT.jar`
>
>`java -jar -Dspring.profiles.active=homol target/publication-manager-1.0-SNAPSHOT.jar`
>
>`java -jar -Dspring.profiles.active=prod target/publication-manager-1.0-SNAPSHOT.jar`

>If you do not want to provide the profile on the command line, you can set the env variable `SPRING_PROFILES_ACTIVE`.
>
> Examples:
>
> `export SPRING_PROFILES_ACTIVE=local`
> 
> `export SPRING_PROFILES_ACTIVE=homol`
> 
> `export SPRING_PROFILES_ACTIVE=prod`

>By default, the `local` profile uses standard logs in the console, and the `homol` profile uses standard logs in the console
> and JSON in rolling files.
>
> If you want to have JSON logs in rolling files  with the `local` profile you can do: 
> 
>`../mvnw clean spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--logging.config=classpath:logback-spring-homol.xml`
>
> or
>
> `java -jar -Dspring.profiles.active=local -Dlogging.config=classpath:logback-spring-homol.xml target/xxx-1.0-SNAPSHOT.jar`


Compile everything:
```
./mvnw clean install
```

Run the Catalog:
```
cd catalog
../mvnw spring-boot:run
```

Run the Catalog Adapter (Reference Implementation):
```
cd catalog-adapter-ri
../mvnw spring-boot:run
```

Run the Publication Manager:
```
cd publication-manager
../mvnw spring-boot:run
```

Run the Publication Adapter (Reference Implementation):
```
cd publication-adapter-ri
../mvnw spring-boot:run
```

Run the Subscription Adapter (Reference Implementation):
```
cd subscription-adapter-ri
../mvnw spring-boot:run
```

Run the Subscription Manager:
```
cd subscription-manager
../mvnw spring-boot:run
```

Run the Operation Manager:
```
cd operation-manager
../mvnw spring-boot:run
```

Run the Operation Adapter (Reference Implementation):
```
cd operation-adapter-ri
../mvnw spring-boot:run
```

Run the Probe:
```
cd probe
../mvnw spring-boot:run
```

Run the Test Server:
```
cd test-server
../mvnw spring-boot:run
```


### Check everything is ok
```
curl http://localhost:8090/tests/nominal/pub/run
```



## ELK (ElasticSearch / Logstash / Kibana)

Install and run ELK using Docker and Docker-compose: https://github.com/JMousqueton/elk-cec-docker

The Logstash input port is `5000` (the port to use as `destination` in the appender in the `logback-spring-<env>.xml` 
config files).

The Kibana port is `5601` (so you can open a browser at `http://localhost:5601`).

Update the `src/main/resources/logback-spring-elastic.xml` file to set the address of Elasticsearch where to send the logs
(update the property `MY_ELASTICSEARCH_URL`. This URL is of the form `<scheme>://<username>:<password>@<host>:<port>`).
> As of now, only the PublicationManager and the SubscriptionManager components have a `logback-spring-elastic.xml` (but
> this config file could be copied to the `src/main/resources/` directory of any component)

To start a component using the logback config to send logs to ElasticSearch, use either (depending on your profile):
- `java -jar -Dspring.profiles.active=local -Dlogging.config=classpath:logback-spring-elastic.xml target/xxx-1.0-SNAPSHOT.jar`
- `java -jar -Dspring.profiles.active=homol -Dlogging.config=classpath:logback-spring-elastic.xml target/xxx-1.0-SNAPSHOT.jar`



## Metricbeat

https://www.elastic.co/guide/en/beats/metricbeat/master/metricbeat-installation-configuration.html

Download metricbeat:
```
curl -L -O https://artifacts.elastic.co/downloads/beats/metricbeat/metricbeat-7.10.2-linux-x86_64.tar.gz
tar xzvf metricbeat-7.10.2-linux-x86_64.tar.gz
```

Edit the `metricbeat.yml` file to set the elastic host and username and password:
```
# ---------------------------- Elasticsearch Output ----------------------------
output.elasticsearch:
  # Array of hosts to connect to.
  hosts: ["localhost:9200"]

  # Protocol - either `http` (default) or `https`.
  #protocol: "https"

  # Authentication credentials - either API key or username/password.
  #api_key: "id:api_key"
  username: "elastic"
  password: "changeme"
```

Metricbeat comes with predefined assets for parsing, indexing, and visualizing your data. To load these assets:
```
./metricbeat setup -e
```

Before starting Metricbeat, modify the user credentials in metricbeat.yml and specify a user who is authorized to publish events.
```
sudo chown root metricbeat.yml 
sudo chown root modules.d/system.yml 
sudo ./metricbeat -e
```

Metricbeat comes with pre-built Kibana dashboards and UIs for visualizing log data. You loaded the dashboards earlier when you ran the setup command.

To open the dashboards, point your browser to `http://localhost:5601`, replacing localhost with the name of the Kibana host.

In the side navigation, click Discover. To see Metricbeat data, make sure the predefined `metricbeat-*` index pattern is selected.

If you don’t see data in Kibana, try changing the time filter to a larger range. By default, Kibana shows the last 15 minutes.

The modules.d directory contains default configurations for all the modules available in Metricbeat. 
To enable or disable specific module configurations under modules.d, run the modules enable or modules disable command.

```
sudo ./metricbeat modules enable prometheus
```

https://www.elastic.co/guide/en/beats/metricbeat/current/configuration-metricbeat.html
Edit the `modules.d\prometheus.yml` file:
- Replace `hosts: ["localhost:9090"]` with `hosts: ["localhost:8083"]` (to collect metric from the Subscription Manager)
- Replace `metrics_path: /metrics` with `metrics_path: /metrics/prometheus`

Set root as the owner of the `prometheus.yml` file:
```
sudo chown root modules.d/prometheus.yml
```

Run metricbeat:
```
sudo ./metricbeat -e
```

Examples of collected metrics:
```
metricset.name = collector

prometheus.labels.instance = localhost:8083
prometheus.labels.job = prometheus

prometheus.labels.component_name = subscription-manager
prometheus.labels.component_instance_id = subscription-manager-0
prometheus.labels.host_name = t460S

prometheus.labels.publication_code = TestSever-Nominal-PUB
prometheus.labels.subscription_code = TestSever-Nominal-SUB
prometheus.labels.event_type_code = TestSever-Nominal-EVT

prometheus.metrics.event_deliveries_attempted_total
prometheus.metrics.event_deliveries_requested_total
prometheus.metrics.event_deliveries_succeeded_total
prometheus.metrics.event_delivery_duration_seconds_count
prometheus.metrics.event_delivery_duration_seconds_max
prometheus.metrics.event_delivery_duration_seconds_sum
```



## Various Tests

### Test of Purge Queue
```
curl --header "Content-Type: application/json" --request POST --data '{"publicationCode": "NominalTest-PUB","payload": { "message": "NominalTest" }, "timeToLiveInSeconds": 180 }' http://localhost:8081/events
curl http://localhost:8087/subscriptions/NominalTest-SUB1/events/next
curl --request DELETE http://localhost:8087/subscriptions/NominalTest-SUB1/events/next
curl --request DELETE http://localhost:8087/subscriptions/NominalTest-SUB1/events
curl http://localhost:8087/subscriptions/NominalTest-SUB1/rabbitmq/queue/info
```


## Kibana

To convert `duration`, `duration_in_sec` and `duration_in_millis` from String to Long, use scripted fileds in Kibana:

```
if (doc['duration_in_millis.keyword'].empty) {
  return 0
} else {
  return Long.parseLong(doc['duration_in_millis.keyword'].value);
}
```


### Visualization Probe - Publication Status (Pie)

```
Type: Pie
Metrics: 
- Slice size: Aggregation: Count
Buckets: 
- Split spice: Aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_PUBLICATION_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_PUBLICATION_FAILED
```


### Visualization Probe - Round trip Status (Pie)
```
Type: Pie
Metrics: 
- Slice size: Aggregation: Count
Buckets: 
- Split spice: Aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_FAILED
```


### Visualization Probe - Publication Status over time (Vertical bars)

```
Type: Vertical bars
Metrics: 
- Y-axis: Aggregation: Count
Buckets:
- X-axis: Aggregation: Date Histogram; Field: @Timestamp
- Split series: Sub-aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_PUBLICATION_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_PUBLICATION_FAILED
```


### Visualization Probe - Round trip Status over time (Vertical bars)

```
Type: Vertical bars
Metrics: 
- Y-axis: Aggregation: Count
Buckets:
- X-axis: Aggregation: Date Histogram; Field: @Timestamp
- Split series: Sub-aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_FAILED
```


### Visualization Probe - Durations (Vertical bars)

```
Type: Vertical bars
Metrics: 
- Y-axis: Aggregation: Count
Buckets:
- X-axis: Aggregation: Range; Field: duration_in_millis_as_numeric
  - Ranges: 0-100, 100-200, 200-300, 300-400, 400-500, 500-1000, 1000-2000, 2000-3000, 3000-4000, 4000-5000, 5000-10000, 10000-infini
- Split series: Sub-aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_FAILED
```


### Visualization Probe - Publications attempted (Metric)

```
Type: Metric
Metrics: 
- Metric: Aggregation: Count
Buckets:
- Split group: Aggregation: Filters
  - Filter 1: Publications attempted
      message_code: PROBE_PUBLICATION_ATTEMPTED
```


### Visualization Probe - Probes received (Metric)

```
Type: Metric
Metrics: 
- Metric: Aggregation: Count
Buckets:
- Split group: Aggregation: Filters
  - Filter 1: Publications attempted
      message_code: PROBE_RECEIVED
```


### Visualization Probe - Round trip Durations over time (Lines)

```
Type: Lines
Metrics: 
- Y-Axis: Agregation: Average; Field: duration_in_millis_as_numeric
Buckets:
- X-axis: Aggregation: Date Histogram; Field: @Timestamp
- Split series: Sub-aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_FAILED
```


### Visualization Probe - Publication Durations over time (Lines)

```
Type: Lines
Metrics: 
- Y-Axis: Agregation: Average; Field: duration_in_millis_as_numeric
Buckets:
- X-axis: Aggregation: Date Histogram; Field: @Timestamp
- Split series: Sub-aggregation: Filters
  - Filter 1: Success
      message_code: PROBE_PUBLICATION_SUCCEEDED
  - Filter 2: Failure
      message_code: PROBE_PUBLICATIONFAILED
```
  
  
