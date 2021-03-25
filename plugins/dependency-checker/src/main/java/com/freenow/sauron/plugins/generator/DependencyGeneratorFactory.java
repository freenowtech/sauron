package com.freenow.sauron.plugins.generator;

import com.freenow.sauron.plugins.ProjectType;
import com.freenow.sauron.properties.PluginsConfigurationProperties;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DependencyGeneratorFactory
{
    public static Optional<DependencyGenerator> newInstance(ProjectType type, PluginsConfigurationProperties properties)
    {
        switch (type)
        {
            case MAVEN:
                return Optional.of(new MavenDependencyGenerator());
            case GRADLE_GROOVY:
                return Optional.of(new GradleGroovyDependencyGenerator());
            case GRADLE_KOTLIN_DSL:
                return Optional.of(new GradleKotlinDslDependencyGenerator());
            case NODEJS:
                return Optional.of(new NodeJsDependencyGenerator(properties));
            case PYTHON_REQUIREMENTS:
                //TODO: Implement PythonRequirementsGenerator
                break;
            case PYTHON_POETRY:
                //TODO: Implement PythonPoetryGenerator
                break;
            case SBT:
                //TODO: Implement SbtGenerator
                break;
            case CLOJURE:
                //TODO: Implement ClojureGenerator
                break;
            case UNKNOWN:
                break;
        }

        return Optional.empty();
    }
}