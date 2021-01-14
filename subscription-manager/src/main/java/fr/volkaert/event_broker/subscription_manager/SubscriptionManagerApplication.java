package fr.volkaert.event_broker.subscription_manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@EntityScan("fr.volkaert")  // Required because entities are not in the same project !
@ComponentScan("fr.volkaert")  // Required because some components/services are not in the same project !
public class SubscriptionManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionManagerApplication.class, args);
    }

    @Autowired
    SubscriptionManagerService subscriptionManagerService;

    @EventListener
    public void handleContextRefreshEvent(ContextStartedEvent ctxStartedEvt) {  subscriptionManagerService.start(); }
}
