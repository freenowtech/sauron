package com.freenow.sauron.handler.impl;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;


@Slf4j
@Setter
public class SpringEventsRequestHandler implements RequestHandler
{
    private final ApplicationEventPublisher applicationEventPublisher;

    private Consumer<BuildRequest> consumer;


    public SpringEventsRequestHandler(ApplicationEventPublisher applicationEventPublisher)
    {
        this.applicationEventPublisher = applicationEventPublisher;
    }


    @Override
    public void handle(BuildRequest request)
    {
        log.debug(String.format("Handling request in Spring Events Request Handler. (%s) %s - %s:%s",
            request.getBuildId(),
            request.getEnvironment(),
            request.getServiceName(),
            request.getCommitId()));

        applicationEventPublisher.publishEvent(request);
    }


    @Async
    @EventListener
    public void consume(BuildRequest request)
    {
        log.debug(String.format("Spring Events Listener consumed a message from events. (%s) %s - %s:%s",
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