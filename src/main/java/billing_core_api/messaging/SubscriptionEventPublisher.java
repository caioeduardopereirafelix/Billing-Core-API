package billing_core_api.messaging;

import billing_core_api.messaging.event.SubscriptionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionEventPublisher {

    private final MessageConverter messageConverter;
    private final RabbitTemplate template;

    public void publishSubscriptionCreated(SubscriptionCreatedEvent event){
        template.convertAndSend(RabbitMQConfig.USER_EXCHANGE,
                                RabbitMQConfig.BINDING_CREATED_ROUTING_KEY,
                                event,
                                message -> {
                                String correlationID = UUID.randomUUID().toString();

                                message.getMessageProperties().setCorrelationId(event.correlationID());

                                return message;
                                });
    }

}
