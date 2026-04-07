package com.freenow.sauron.plugins;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.Set;

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
    GO,
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
        if (hasGoMod(repositoryPath))
        {
            return GO;
        }
        return UNKNOWN;
    }


    public boolean hasNullGroup()
    {
        return this.equals(ProjectType.NODEJS) ||
            this.equals(ProjectType.PYTHON_POETRY) ||
            this.equals(ProjectType.PYTHON_REQUIREMENTS) ||
            this.equals(ProjectType.GO);
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
            case GO:
                return "org.golang";
            default:
                return "";
        }
    }


    private static boolean hasGoMod(Path repositoryPath)
    {
        return findGoMod(repositoryPath).isPresent();
    }


    public static Optional<Path> findGoMod(Path root)
    {
        if (Files.exists(root.resolve("go.mod")))
        {
            return Optional.of(root);
        }

        Set<String> excluded = Set.of(".git", "vendor", "kustomize");
        Path[] result = new Path[1];

        try
        {
            Files.walkFileTree(
                root, new SimpleFileVisitor<>()
                {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    {
                        return excluded.contains(String.valueOf(dir.getFileName()))
                            ? FileVisitResult.SKIP_SUBTREE
                            : FileVisitResult.CONTINUE;
                    }


                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    {
                        if ("go.mod".equals(file.getFileName().toString()))
                        {
                            result[0] = file.getParent();
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        }
        catch (IOException ignored)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(result[0]);
    }
}
