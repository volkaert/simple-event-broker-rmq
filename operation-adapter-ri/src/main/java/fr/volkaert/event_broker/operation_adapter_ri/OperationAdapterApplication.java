package fr.volkaert.event_broker.operation_adapter_ri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@EntityScan("fr.volkaert")  // Required because entities are not in the same project !
@ComponentScan("fr.volkaert")  // Required because some components/services are not in the same project !
public class OperationAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperationAdapterApplication.class, args);
    }
}
