package billing_core_api.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class RabbitMQConfig {

    private static final String USER_EXCHANGE = "subscription.exchange";
    private static final String QUEUE_SUCESS_SUBSCRIPTION = "welcome.subscription";
    private static final String BINDING_CREATED_ROUTING_KEY = "success.subscription";

    @Bean
    public TopicExchange exchange(){
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public Queue queue(){
        return  QueueBuilder
                .durable(QUEUE_SUCESS_SUBSCRIPTION)
                .build();
    }

    @Bean
    public Binding binding(){
        return BindingBuilder
                .bind(queue())
                .to(exchange())
                .with(BINDING_CREATED_ROUTING_KEY);
    }
}
