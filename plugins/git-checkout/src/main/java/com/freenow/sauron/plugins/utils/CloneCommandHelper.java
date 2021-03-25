package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.Optional;

public final class CloneCommandHelper
{
    private CloneCommandHelper()
    {
        throw new UnsupportedOperationException();
    }


    public static Optional<String> getStringProperty(String key, PluginsConfigurationProperties properties)
    {
        return properties.getPluginConfigurationProperty("git-checkout", key)
            .filter(String.class::isInstance)
            .map(String.class::cast);
    }
}