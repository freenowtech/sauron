package com.freenow.sauron.plugins;

import java.util.Arrays;

public enum KubernetesResources
{
    POD,
    SERVICE,
    DEPLOYMENT,
    INGRESS,
    CRONJOB,
    JOB,
    HORIZONTALPODAUTOSCALER,
    UNKNOWN;


    public static KubernetesResources fromString(String value)
    {
        return Arrays.stream(KubernetesResources.values())
            .filter(kubernetesResources -> kubernetesResources.name().equalsIgnoreCase(value))
            .findFirst()
            .orElse(UNKNOWN);
    }
}