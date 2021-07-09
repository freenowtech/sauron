package com.freenow.sauron.plugins;

import java.nio.file.Files;
import java.nio.file.Path;

public enum ProjectType
{
    MAVEN,
    GRADLE_GROOVY,
    GRADLE_KOTLIN_DSL,
    NODEJS,
    PYTHON_REQUIREMENTS,
    PYTHON_POETRY,
    SBT,
    CLOJURE,
    UNKNOWN;


    public static ProjectType fromPath(Path repositoryPath)
    {
        if (Files.exists(repositoryPath.resolve("pom.xml")))
        {
            return MAVEN;
        }
        if (Files.exists(repositoryPath.resolve("build.gradle")))
        {
            return GRADLE_GROOVY;
        }
        if (Files.exists(repositoryPath.resolve("build.gradle.kts")))
        {
            return GRADLE_KOTLIN_DSL;
        }
        if (Files.exists(repositoryPath.resolve("package.json")))
        {
            return NODEJS;
        }
        if (Files.exists(repositoryPath.resolve("pyproject.toml")))
        {
            return PYTHON_POETRY;
        }
        if (Files.exists(repositoryPath.resolve("requirements.txt")))
        {
            return PYTHON_REQUIREMENTS;
        }
        if (Files.exists(repositoryPath.resolve("build.sbt")))
        {
            return SBT;
        }
        if (Files.exists(repositoryPath.resolve("project.clj")))
        {
            return CLOJURE;
        }

        return UNKNOWN;
    }


    public boolean hasNullGroup()
    {
        return this.equals(ProjectType.NODEJS) || this.equals(ProjectType.PYTHON_POETRY) || this.equals(ProjectType.PYTHON_REQUIREMENTS);
    }


    public String defaultGroup()
    {
        switch (this)
        {
            case NODEJS:
                return "org.npmjs";
            case PYTHON_REQUIREMENTS:
            case PYTHON_POETRY:
                return "org.python";
            default:
                return "";
        }
    }
}