package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import io.kubernetes.client.openapi.models.V1Container;

public interface ContainerCheckStrategy {
    String getName();
    void check(V1Container container, DataSet input);
}


