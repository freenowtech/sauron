package com.freenow.sauron.plugins.generator.gradle;

import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.ConnectorServices;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;

@Slf4j
public abstract class GradleDependencyGenerator extends DependencyGenerator
{

    protected GradleDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
    }


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
        DefaultGradleConnector connector = ConnectorServices.createConnector();
        connector.forProjectDirectory(repositoryPath.toFile());
        connector.daemonMaxIdleTime(commandTimeoutMinutes, TimeUnit.MINUTES);
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

        if (Pattern.compile("plugins\\s*\\{[^}]*}").matcher(content).find())
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