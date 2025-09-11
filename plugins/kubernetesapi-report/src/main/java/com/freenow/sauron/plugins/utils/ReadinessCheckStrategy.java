package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.readers.KubernetesContainersReader;
import io.kubernetes.client.openapi.models.V1Container;

public class ReadinessCheckStrategy implements ContainerCheckStrategy
{
    @Override
    public String getName()
    {
        return KubernetesContainersReader.READINESS;
    }


    @Override
    public void check(V1Container container, DataSet input)
    {
        boolean hasReadiness = container.getReadinessProbe() != null;
        input.setAdditionalInformation(getName(), String.valueOf(hasReadiness));
    }
}
