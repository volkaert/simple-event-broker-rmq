INSERT INTO event_type (code, name, active) VALUES ('NominalTest-EVT', 'NominalTest-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('Failure401Test-EVT', 'Failure401Test-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('Failure500Test-EVT', 'Failure500Test-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('SlowTest-EVT', 'SlowTest-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('ComplexPayloadTest-EVT', 'ComplexPayloadTest-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('ComplexPayload2Test-EVT', 'ComplexPayload2Test-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('TimeToLiveTest-EVT', 'TimeToLiveTest-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('OAuth2Test-EVT', 'OAuth2Test-EVT', true)
INSERT INTO event_type (code, name, active) VALUES ('Probe-EVT', 'Probe-EVT', true)

INSERT INTO publication (code, name, event_type_code, active) VALUES ('NominalTest-PUB', 'NominalTest-PUB', 'NominalTest-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('Failure401Test-PUB', 'Failure401Test-PUB', 'Failure401Test-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('Failure500Test-PUB', 'Failure500Test-PUB', 'Failure500Test-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('SlowTest-PUB', 'SlowTest-PUB', 'SlowTest-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('ComplexPayloadTest-PUB', 'ComplexPayloadTest-PUB', 'ComplexPayloadTest-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('ComplexPayload2Test-PUB', 'ComplexPayload2Test-PUB', 'ComplexPayload2Test-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('TimeToLiveTest-PUB', 'TimeToLiveTest-PUB', 'TimeToLiveTest-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('OAuth2Test-PUB', 'OAuth2Test-PUB', 'OAuth2Test-EVT', true)
INSERT INTO publication (code, name, event_type_code, active) VALUES ('Probe-PUB', 'Probe-PUB', 'Probe-EVT', true)

INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, webhook_headers, auth_client_id, auth_client_secret, secret) VALUES ('NominalTest-SUB1', 'NominalTest-SUB1', 'NominalTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'header1:value1', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, webhook_headers, auth_client_id, auth_client_secret, secret) VALUES ('NominalTest-SUB2', 'NominalTest-SUB2', 'NominalTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'header1 : value1 ; header2 : value2', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('Failure401Test-SUB1-Ok', 'Failure401Test-SUB1-Ok', 'Failure401Test-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('Failure401Test-SUB2-Failure401', 'Failure401Test-SUB2-Failure401', 'Failure401Test-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/failure401', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('Failure500Test-SUB1-Ok', 'Failure500Test-SUB1-Ok', 'Failure500Test-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('Failure500Test-SUB2-Failure500', 'Failure500Test-SUB2-Failure500', 'Failure500Test-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/failure500', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('SlowTest-SUB1-Ok', 'SlowTest-SUB1-Ok', 'SlowTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('SlowTest-SUB2-Slow', 'SlowTest-SUB2-Slow', 'SlowTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/slow', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('ComplexPayloadTest-SUB1', 'ComplexPayloadTest-SUB1', 'ComplexPayloadTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/complex-payload', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('ComplexPayload2Test-SUB1', 'ComplexPayload2Test-SUB1', 'ComplexPayload2Test-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/complex-payload2', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret) VALUES ('TimeToLiveTest-SUB-Ok', 'TimeToLiveTest-SUB-Ok', 'TimeToLiveTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret, time_to_live_in_seconds_for_webhook_connection_error) VALUES ('TimeToLiveTest-SUB-ConnectionFailed', 'TimeToLiveTest-SUB-ConnectionFailed', 'TimeToLiveTest-EVT', true, NULL, 'http://THIS_SEVER_DOES_NOT_EXIST:8099/tests/subscriber1/nominal', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL, 30)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret, time_to_live_in_seconds_for_webhook_read_timeout_error) VALUES ('TimeToLiveTest-SUB-ReadTimeout', 'TimeToLiveTest-SUB-ReadTimeout', 'TimeToLiveTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/slow', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL, 30)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret, time_to_live_in_seconds_for_webhook_read_timeout_error) VALUES ('TimeToLiveTest-SUB-ReadTimeout', 'TimeToLiveTest-SUB-ReadTimeout', 'TimeToLiveTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/slow', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL, 30)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret, time_to_live_in_seconds_for_webhook_server5xx_error) VALUES ('TimeToLiveTest-SUB-Failure500', 'TimeToLiveTest-SUB-Failure500', 'TimeToLiveTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/failure500', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL, 30)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, auth_client_id, auth_client_secret, secret, time_to_live_in_seconds_for_webhook_client4xx_error) VALUES ('TimeToLiveTest-SUB-Failure401', 'TimeToLiveTest-SUB-Failure401', 'TimeToLiveTest-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/failure401', 'application/json', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL, 30)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, webhook_headers, auth_method, auth_scope, secret) VALUES ('OAuth2Test-SUB', 'OAuth2Test-SUB', 'OAuth2Test-EVT', true, NULL, 'http://localhost:8099/tests/subscriber1/nominal', 'application/json', 'header1:value1', 'oauth2', 'test_subscriber_oauth2.webhooks', NULL)
INSERT INTO subscription (code, name, event_type_code, active, channel, webhook_url, webhook_content_type, webhook_headers, auth_client_id, auth_client_secret, secret) VALUES ('Probe-SUB', 'Probe-SUB', 'Probe-EVT', true, NULL, 'http://localhost:8100/probe/webhook', 'application/json', 'header1:value1', 'some-client-id-for-webhook', 'some-client-secret-for-webhook', NULL)
