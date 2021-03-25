package com.freenow.sauron.plugins.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;


@Slf4j
public abstract class GradleDependencyGenerator implements DependencyGenerator
{

    protected abstract String gradleFile();

    protected abstract String cycloneDxPlugin();

    @Override
    public Path generateCycloneDxBom(Path repositoryPath)
    {
        try
        {
            injectCycloneDxPlugin(repositoryPath);
            runGradleTask(repositoryPath, "cyclonedxBom");
            return repositoryPath.resolve("build/reports/bom.xml");
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return null;
    }


    private void runGradleTask(Path repositoryPath, String... tasks)
    {
        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(repositoryPath.toFile());
        try (ProjectConnection connection = connector.connect())
        {
            BuildLauncher build = connection.newBuild();
            build.forTasks(tasks);
            build.run();
        }
    }


    private void injectCycloneDxPlugin(Path repositoryPath) throws IOException
    {
        Path buildGradlePath = repositoryPath.resolve(gradleFile());
        String content = new String(Files.readAllBytes(buildGradlePath));

        if(Pattern.compile("plugins\\s*\\{[^}]*}").matcher(content).find())
        {
            content = content.replaceAll("plugins\\s*\\{([^}]*)}", String.format("plugins {$1\n%s\n}", cycloneDxPlugin()));
        }
        else
        {
            content = String.format("plugins { %s }\n%s", cycloneDxPlugin(), content);
        }

        Files.write(buildGradlePath, content.getBytes());
    }
}