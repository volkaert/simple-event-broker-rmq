package fr.volkaert.event_broker.probe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan("fr.volkaert")  // Required because entities are not in the same project !
@ComponentScan("fr.volkaert")  // Required because some components/services are not in the same project !
public class ProbeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProbeApplication.class, args);
	}
}
