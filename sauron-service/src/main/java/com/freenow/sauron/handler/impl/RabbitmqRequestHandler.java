package com.freenow.sauron.handler.impl;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.freenow.sauron.utils.Constants.SAURON_QUEUE_NAME;

@Slf4j
@Setter
public class RabbitmqRequestHandler implements RequestHandler
{
    private static final String ASYNC_CONTAINER_FACTORY = "eventBus";

    private final RabbitTemplate rabbitTemplate;

    private Consumer<BuildRequest> consumer;


    public RabbitmqRequestHandler(RabbitTemplate rabbitTemplate)
    {
        this.rabbitTemplate = rabbitTemplate;
    }


    @Override
    public void handle(BuildRequest request)
    {
        log.debug(String.format("Handling request in Rabbitmq Request Handler. (%s) %s - %s:%s",
            request.getBuildId(),
            request.getEnvironment(),
            request.getServiceName(),
            request.getCommitId()));

        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), ASYNC_CONTAINER_FACTORY);
        rabbitTemplate.convertAndSend(SAURON_QUEUE_NAME, request);
        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
    }


    @RabbitListener(
        containerFactory = ASYNC_CONTAINER_FACTORY,
        queues = SAURON_QUEUE_NAME
    )
    public void consume(BuildRequest request)
    {
        log.debug(String.format("Rabbitmq consumed message from the queue. (%s) %s - %s:%s",
            request.getBuildId(),
            request.getEnvironment(),
            request.getServiceName(),
            request.getCommitId()));

        if (consumer != null)
        {
            consumer.accept(request);
        }
        else
        {
            log.error("Consumer is not set. Message will be discarded.");
        }
    }
}