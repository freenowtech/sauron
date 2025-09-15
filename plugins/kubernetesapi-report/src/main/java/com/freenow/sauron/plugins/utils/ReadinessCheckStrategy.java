package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import io.kubernetes.client.openapi.models.V1Container;

import static com.freenow.sauron.plugins.readers.KubernetesContainersReader.READINESS;

public class ReadinessCheckStrategy implements ContainerCheckStrategy
{

    @Override
    public String getName()
    {
        return READINESS;
    }


    @Override
    public void check(V1Container container, DataSet input)
    {
        boolean hasReadiness = container.getReadinessProbe() != null;
        input.setAdditionalInformation(getName(), hasReadiness);
    }
}
