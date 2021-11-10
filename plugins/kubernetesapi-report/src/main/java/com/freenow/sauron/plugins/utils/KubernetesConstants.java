package com.freenow.sauron.plugins.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class KubernetesConstants
{
    public static final String K8S_DEFAULT_NAMESPACE = "default";

    public static final int K8S_API_TIMEOUT_SECONDS = 5;

    public static final String K8S_PRETTY_OUTPUT = Boolean.FALSE.toString();
}
