package com.freenow.sauron.plugins.generator.gradle;

import com.freenow.sauron.properties.PluginsConfigurationProperties;

public class GradleKotlinDslDependencyGenerator extends GradleDependencyGenerator
{
    public GradleKotlinDslDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
    }


    @Override
    protected String gradleFile()
    {
        return "build.gradle.kts";
    }


    @Override
    protected String cycloneDxPlugin()
    {
        return "id(\"org.cyclonedx.bom\") version \"1.1.4\"";
    }
}