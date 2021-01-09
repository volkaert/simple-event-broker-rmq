package fr.volkaert.event_broker.telemetry;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@ConfigurationProperties(prefix = "broker.telemetry")
@Data
public class TelemetryConfig {

    String componentName;
    String componentInstanceId;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("component_name", componentName)
                .commonTags("component_instance_id", componentInstanceId)
                .commonTags("host_name", getHostName());
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
