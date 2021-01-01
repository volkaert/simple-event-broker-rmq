package fr.volkaert.event_broker.publication_manager.config;

import fr.volkaert.event_broker.publication_manager.config.BrokerConfig;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

@Configuration
public class RabbitConfig implements RabbitListenerConfigurer {

    @Autowired
    BrokerConfig config;

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter(consumerJackson2MessageConverter());
        return messageHandlerMethodFactory;
    }

    @Bean
    public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
        return new MappingJackson2MessageConverter();
    }

    @Bean
    @Qualifier("DefaultConnectionFactory")
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(config.getRabbitMQHost(), config.getRabbitMQPort());
        connectionFactory.setUsername(config.getRabbitMQUsername());
        connectionFactory.setUsername(config.getRabbitMQPassword());
        return connectionFactory;
    }

    @Bean
    @Qualifier("ConnectionFactoryForMirroring")
    public ConnectionFactory connectionFactoryForMirroring() {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(config.getRabbitMQHostForMirroring(), config.getRabbitMQPortForMirroring());
        connectionFactory.setUsername(config.getRabbitMQUsernameForMirroring());
        connectionFactory.setUsername(config.getRabbitMQPasswordForMirroring());
        return connectionFactory;
    }

    @Bean
    @Qualifier("DefaultAMQPAdmin")
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    @Qualifier("AMQPAdminForMirroring")
    public AmqpAdmin amqpAdminForMirroring() {
        return new RabbitAdmin(connectionFactoryForMirroring());
    }

    @Bean
    @Qualifier("DefaultRabbitTemplate")
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    @Qualifier("RabbitTemplateForMirroring")
    public RabbitTemplate rabbitTemplateForMirroring() {
        RabbitTemplate template = new RabbitTemplate(connectionFactoryForMirroring());
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter()  {
        return new Jackson2JsonMessageConverter();
    }
}

