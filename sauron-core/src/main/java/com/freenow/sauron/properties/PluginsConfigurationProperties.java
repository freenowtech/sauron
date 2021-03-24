package com.freenow.sauron.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sauron.plugins")
public class PluginsConfigurationProperties extends HashMap<String, Map<String, Object>>
{
    public Optional<Object> getPluginConfigurationProperty(String pluginId, String property)
    {
        return Optional.ofNullable(this.getOrDefault(pluginId, Collections.emptyMap()).getOrDefault(property, null));
    }
}
