package fr.volkaert.event_broker.operation_manager;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@ComponentScan("fr.volkaert")  // Required because some components/services are not in the same project !
public class OperationManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationManagerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OperationManagerApplication.class, args);
    }
}