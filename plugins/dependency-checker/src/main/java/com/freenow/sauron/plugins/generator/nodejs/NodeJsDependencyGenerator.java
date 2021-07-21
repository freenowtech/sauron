package com.freenow.sauron.plugins.generator.nodejs;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.plugins.command.NonZeroExitCodeException;
import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeJsDependencyGenerator implements DependencyGenerator
{
    public static class PackageLockJsonMissingException extends IllegalStateException
    {
        private PackageLockJsonMissingException()
        {
            super("Project is missing package-lock.json");
        }
    }

    public static class YarnNotSupportedException extends IllegalStateException
    {
        private YarnNotSupportedException(String message)
        {
            super("Yarn is not supported: " + message);
        }
    }

    private String npmBin = "npm";
    private String npxBin = "npx";


    public NodeJsDependencyGenerator(PluginsConfigurationProperties properties)
    {
        properties.getPluginConfigurationProperty("dependency-checker", "nodejs")
            .ifPresent(nodeJsConfig ->
            {
                if (nodeJsConfig instanceof Map)
                {
                    Map<String, Object> config = (Map<String, Object>) nodeJsConfig;
                    this.npmBin = (String) config.getOrDefault("npm", npmBin);
                    this.npxBin = (String) config.getOrDefault("npx", npxBin);
                }
                else
                {
                    log.warn("Config sauron.plugins.dependency-checker.nodejs is malformed, expected map.");
                }
            });
    }


    @Override
    public Path generateCycloneDxBom(Path repositoryPath)
    {
        try
        {
            npmInstall(repositoryPath);
            return buildCycloneDxBom(repositoryPath);
        }
        catch (IllegalStateException e)
        {
            log.info("Skip building Cyclone DX BOM: {}", e.getMessage());
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return null;
    }


    private void npmInstall(Path repositoryPath) throws IOException, InterruptedException, YarnNotSupportedException, PackageLockJsonMissingException, NonZeroExitCodeException
    {
        requireNotYarn(repositoryPath);
        requirePackageLockJson(repositoryPath);
        Command.builder()
            .repositoryPath(repositoryPath)
            .commandline(String.join(" ", npmBin, "ci"))
            .build()
            .run();
    }


    private Path buildCycloneDxBom(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException
    {
        Command.builder()
            .repositoryPath(repositoryPath)
            .commandline(String.join(" ", npxBin, "@cyclonedx/bom"))
            .build()
            .run();
        return repositoryPath.resolve("bom.xml");
    }


    private void requireNotYarn(Path repositoryPath) throws YarnNotSupportedException
    {
        if (Files.exists(repositoryPath.resolve("yarn.lock")))
        {
            throw new YarnNotSupportedException("Found yarn.lock file");
        }
    }


    private void requirePackageLockJson(Path repositoryPath) throws PackageLockJsonMissingException
    {
        if (Files.notExists(repositoryPath.resolve("package-lock.json")))
        {
            throw new PackageLockJsonMissingException();
        }
    }
}
