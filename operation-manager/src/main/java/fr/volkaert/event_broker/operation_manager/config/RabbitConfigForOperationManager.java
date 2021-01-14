package fr.volkaert.event_broker.operation_manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

@Configuration
public class RabbitConfigForOperationManager implements RabbitListenerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConfigForOperationManager.class);

    @Autowired
    BrokerConfigForOperationManager config;

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
    public ConnectionFactory connectionFactory() {
        LOGGER.info("RabbitMQ config is host:{}, port:{}, username:{}, ssl:{}",
                config.getRabbitMQHost(),
                config.getRabbitMQPort(),
                config.getRabbitMQUsername(),
                config.isRabbitMQSSLEnabled());

        com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        cf.setHost(config.getRabbitMQHost());
        cf.setPort(config.getRabbitMQPort());
        cf.setUsername(config.getRabbitMQUsername());
        cf.setPassword(config.getRabbitMQPassword());

        cf.setAutomaticRecoveryEnabled(false);
        // explicitly set to false otherwise the following  warning is displayed by Spring Boot:
        //Automatic Recovery was Enabled in the provided connection factory;
        //while Spring AMQP is generally compatible with this feature, there
        //are some corner cases where problems arise. Spring AMQP
        // prefers to use its own recovery mechanisms; when this option is true, you may receive
        // 'AutoRecoverConnectionNotCurrentlyOpenException's until the connection is recovered.
        // It has therefore been disabled; if you really wish to enable it, use
        //'getRabbitConnectionFactory().setAutomaticRecoveryEnabled(true)',
        //        but this is discouraged.

        if (config.isRabbitMQSSLEnabled()) {
            try {
                cf.useSslProtocol();
            } catch (Exception ex) {
                LOGGER.error("Error while configuring SSL for RabbitMQ", ex);
            }
        }

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(cf);
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter()  {
        return new Jackson2JsonMessageConverter();
    }
}

