package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.readers.KubernetesContainersReader;
import io.kubernetes.client.openapi.models.V1Container;

public class LivenessCheckStrategy implements ContainerCheckStrategy
{
    @Override
    public String getName() { return KubernetesContainersReader.LIVENESS; }

    @Override
    public void check(V1Container container, DataSet input) {
        boolean hasLiveness = container.getLivenessProbe() != null;
        input.setAdditionalInformation(getName(), String.valueOf(hasLiveness));
    }
}
