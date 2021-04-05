package com.freenow.sauron.handler;

import com.freenow.sauron.datatransferobject.BuildRequest;
import java.util.function.Consumer;

public interface RequestHandler
{
    void handle(BuildRequest request);

    void setConsumer(Consumer<BuildRequest> consumer);

    void consume(BuildRequest buildRequest);
}
