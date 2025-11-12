package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import io.kubernetes.client.openapi.models.V1Container;

import static com.freenow.sauron.plugins.readers.KubernetesContainersReader.LIVENESS;

public class LivenessCheckStrategy implements ContainerCheckStrategy
{
    @Override
    public String getName()
    {
        return LIVENESS;
    }


    @Override
    public void check(V1Container container, DataSet input)
    {
        boolean hasLiveness = container.getLivenessProbe() != null;
        input.setAdditionalInformation(getName(), hasLiveness);
    }
}
