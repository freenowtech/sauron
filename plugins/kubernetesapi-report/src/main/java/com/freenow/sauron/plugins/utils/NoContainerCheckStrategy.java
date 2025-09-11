package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.plugins.readers.KubernetesContainersReader;
import io.kubernetes.client.openapi.models.V1Container;
import com.freenow.sauron.model.DataSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoContainerCheckStrategy implements ContainerCheckStrategy
{
    @Override
    public String getName()
    {
        return KubernetesContainersReader.NOCHECK;
    }


    @Override
    public void check(V1Container container, DataSet input)
    {
        log.warn("No check implementation for strategy: {}", this.getClass().getSimpleName());
    }
}
