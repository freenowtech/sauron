package com.freenow.sauron.plugins.generator.gradle;

public class GradleKotlinDslDependencyGenerator extends GradleDependencyGenerator
{
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