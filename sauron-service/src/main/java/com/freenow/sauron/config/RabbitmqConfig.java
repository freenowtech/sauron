package com.freenow.sauron.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.freenow.sauron.utils.Constants.SAURON_QUEUE_NAME;

@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitmqConfig
{
    @Bean
    public Queue sauronQueue()
    {
        return new Queue(SAURON_QUEUE_NAME, true);
    }


    @Bean
    public SimpleRabbitListenerContainerFactory eventBusPrefetchCount(@Qualifier("eventBus") SimpleRabbitListenerContainerFactory eventBus)
    {
        eventBus.setPrefetchCount(1);
        return eventBus;
    }
}