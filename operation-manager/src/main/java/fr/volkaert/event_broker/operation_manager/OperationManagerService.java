package fr.volkaert.event_broker.operation_manager;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import fr.volkaert.event_broker.model.InflightEvent;
import fr.volkaert.event_broker.util.RabbitMQNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class OperationManagerService {
    @Autowired
    ConnectionFactory rabbitMQConnectionFactory;

    @Autowired
    Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationManagerService.class);

    public InflightEvent getLastEventForSubscription(String subscriptionCode) {
        return getOrDeleteLastEventForSubscription(subscriptionCode, false);
    }

    public InflightEvent deleteLastEventForSubscription(String subscriptionCode) {
        return getOrDeleteLastEventForSubscription(subscriptionCode, true);
    }

    private InflightEvent getOrDeleteLastEventForSubscription(String subscriptionCode, boolean ack) {
        InflightEvent lastEvent = null;
        String queueNameForSubscription = RabbitMQNames.getQueueNameForSubscription(subscriptionCode);
        MyRabbitTemplate rabbitTemplate = new MyRabbitTemplate(rabbitMQConnectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        Message message = rabbitTemplate.execute(channel -> {
            GetResponse response = channel.basicGet(queueNameForSubscription, ack);
            if (response != null) {
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                if (ack)
                    channel.basicAck(deliveryTag, false);   // false: ack of the last message only
                else
                    channel.basicReject(deliveryTag, true); // true: requeue the last message
                Message msg = rabbitTemplate.myOwnBuildMessageFromResponse(response);
                return msg;
            }
            return null;
        });
        rabbitTemplate.destroy();
        if (message != null) {
            lastEvent = (InflightEvent)jackson2JsonMessageConverter.fromMessage(message);
        }
        return lastEvent;
    }

    public void deleteAllEventsForSubscription(String subscriptionCode) {
        while (deleteLastEventForSubscription(subscriptionCode) != null) {
        }
    }

    // Ugly !!! But this is the only way I found to have access to those fucking methods !!!
    private static class MyRabbitTemplate extends RabbitTemplate {
        public MyRabbitTemplate(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }
        public Message myOwnBuildMessageFromResponse(GetResponse response) {
            return myOwnBuildMessage(response.getEnvelope(), response.getProps(), response.getBody(), response.getMessageCount());
        }

        private Message myOwnBuildMessage(Envelope envelope, AMQP.BasicProperties properties, byte[] body, int msgCount) {
            MessageProperties messageProps = this.getMessagePropertiesConverter().toMessageProperties(properties, envelope, this.getEncoding());
            if (msgCount >= 0) {
                messageProps.setMessageCount(msgCount);
            }
            Message message = new Message(body, messageProps);
            MessagePostProcessor processor;
            if (this.getAfterReceivePostProcessors() != null) {
                for (Iterator iter = this.getAfterReceivePostProcessors().iterator(); iter.hasNext(); message = processor.postProcessMessage(message)) {
                    processor = (MessagePostProcessor)iter.next();
                }
            }
            return message;
        }
    }
}
