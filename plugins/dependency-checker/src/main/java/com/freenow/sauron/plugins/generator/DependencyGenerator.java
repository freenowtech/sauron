package com.freenow.sauron.plugins.generator;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.nio.file.Path;

public abstract class DependencyGenerator
{
    private static final Integer DEFAULT_COMMAND_TIMEOUT_MINUTES = 10;

    protected final Integer commandTimeoutMinutes;


    protected DependencyGenerator(PluginsConfigurationProperties properties)
    {
        commandTimeoutMinutes = properties.getPluginConfigurationProperty("dependency-checker", "commandTimeoutMinutes")
            .map(Integer.class::cast)
            .orElse(DEFAULT_COMMAND_TIMEOUT_MINUTES);
    }


    public abstract Path generateCycloneDxBom(Path repositoryPath);
}
