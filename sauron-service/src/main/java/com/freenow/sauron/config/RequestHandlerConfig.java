package com.freenow.sauron.config;

import com.freenow.sauron.handler.RequestHandler;
import com.freenow.sauron.handler.impl.RabbitmqRequestHandler;
import com.freenow.sauron.handler.impl.SpringEventsRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RequestHandlerConfig
{
    @Bean
    @ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true")
    public RequestHandler rabbitmqRequestHandler(RabbitTemplate template, RabbitListenerEndpointRegistry registry)
    {
        log.info("Rabbitmq is enabled as request handler. Your pipeline build requests will be queued in Rabbitmq.");
        return new RabbitmqRequestHandler(template, registry);
    }


    @Bean
    @ConditionalOnMissingBean(RequestHandler.class)
    @ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "false", matchIfMissing = true)
    public RequestHandler springEventRequestHandler(ApplicationEventPublisher applicationEventPublisher)
    {
        log.info("Spring events request handler is enabled. You pipeline build requests will be processed by spring event dispatcher thread pool.");
        return new SpringEventsRequestHandler(applicationEventPublisher);
    }
}