package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import org.pf4j.ExtensionPoint;

public interface SauronExtension extends ExtensionPoint
{
    DataSet apply(PluginsConfigurationProperties properties, DataSet input);
}
