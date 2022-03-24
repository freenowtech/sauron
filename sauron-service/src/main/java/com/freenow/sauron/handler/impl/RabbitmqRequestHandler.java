package com.freenow.sauron.handler.impl;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import com.freenow.sauron.plugins.PluginsLoadedEvent;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.SimpleResourceHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.event.EventListener;

import static com.freenow.sauron.utils.Constants.SAURON_QUEUE_NAME;

@Slf4j
@Setter
public class RabbitmqRequestHandler implements RequestHandler
{
    private static final String ASYNC_CONTAINER_FACTORY = "eventBus";

    private static final String LISTENER_ID = "sauron-consumer";

    private static final String AUTOSTARTUP = "false";

    private final RabbitTemplate rabbitTemplate;

    private final RabbitListenerEndpointRegistry registry;

    private Consumer<BuildRequest> consumer;


    public RabbitmqRequestHandler(RabbitTemplate rabbitTemplate, RabbitListenerEndpointRegistry registry)
    {
        this.rabbitTemplate = rabbitTemplate;
        this.registry = registry;
    }


    @Override
    public void handle(BuildRequest request)
    {
        log.debug(String.format(
            "Handling request in Rabbitmq Request Handler. (%s) %s - %s:%s",
            request.getBuildId(),
            request.getEnvironment(),
            request.getServiceName(),
            request.getCommitId()));

        try
        {
            SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), ASYNC_CONTAINER_FACTORY);
        }
        catch (Exception ignored)
        {
            log.info("Skipping bind since it is already bind");
        }

        rabbitTemplate.convertAndSend(SAURON_QUEUE_NAME, request);

        try
        {
            SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
        }
        catch (Exception ignored)
        {
            log.info("Skipping unbind since it is already unbind");
        }
    }


    @RabbitListener(
        id = LISTENER_ID,
        containerFactory = ASYNC_CONTAINER_FACTORY,
        queues = SAURON_QUEUE_NAME,
        autoStartup = AUTOSTARTUP
    )
    public void consume(BuildRequest request)
    {
        log.debug(String.format(
            "Rabbitmq consumed message from the queue. (%s) %s - %s:%s",
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


    @EventListener
    public void onPluginLoadedEvent(PluginsLoadedEvent event)
    {
        if (event.isSuccess())
        {
            registry.getListenerContainer(LISTENER_ID).start();
        }
        else
        {
            registry.getListenerContainer(LISTENER_ID).stop();
        }
    }
}