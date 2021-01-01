package fr.volkaert.event_broker.test_server;

import fr.volkaert.event_broker.test_server.config.TestServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class TestServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestServerApplication.class, args);
    }

    @Autowired
    TestServerConfig config;

    @Bean
    @Qualifier("ExecutorServiceForTests")
    public ExecutorService createExecutorServiceForTests() {
        return Executors.newFixedThreadPool(config.getThreadPoolSize());
    }
}
