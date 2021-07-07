package com.freenow.sauron.plugins.generator.gradle;

public class GradleGroovyDependencyGenerator extends GradleDependencyGenerator
{
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