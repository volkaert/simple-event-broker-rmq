package fr.volkaert.event_broker.publication_manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@ComponentScan("fr.volkaert")  // Required because some components/services are not in the same project !
public class PublicationManagerApplication {

    @Autowired
    BrokerConfig config;

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationManagerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PublicationManagerApplication.class, args);
    }
}
