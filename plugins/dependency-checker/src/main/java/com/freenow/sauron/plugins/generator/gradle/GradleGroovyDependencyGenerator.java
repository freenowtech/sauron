package com.freenow.sauron.plugins.generator.gradle;

import com.freenow.sauron.properties.PluginsConfigurationProperties;

public class GradleGroovyDependencyGenerator extends GradleDependencyGenerator
{
    public GradleGroovyDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
    }


    @Override
    protected String gradleFile()
    {
        return "build.gradle";
    }


    @Override
    protected String cycloneDxPlugin()
    {
        return "id \"org.cyclonedx.bom\" version \"1.1.4\"";
    }
}