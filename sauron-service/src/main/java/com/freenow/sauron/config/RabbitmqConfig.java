package com.freenow.sauron.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
    public SimpleRabbitListenerContainerFactory eventBusPrefetchCount(
        @Qualifier("eventBus") SimpleRabbitListenerContainerFactory eventBus,
        final Jackson2JsonMessageConverter converter)
    {
        eventBus.setPrefetchCount(1);
        eventBus.setMessageConverter(converter);
        return eventBus;
    }


    @Bean
    public RabbitTemplate rabbitTemplate(
        final ConnectionFactory multiRabbitConnectionFactory,
        final Jackson2JsonMessageConverter converter)
    {
        final var rabbitTemplate = new RabbitTemplate(multiRabbitConnectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }


    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter()
    {
        return new Jackson2JsonMessageConverter();
    }
}