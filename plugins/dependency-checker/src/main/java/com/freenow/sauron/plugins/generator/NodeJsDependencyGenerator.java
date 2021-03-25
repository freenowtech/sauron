package com.freenow.sauron.plugins.generator;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class NodeJsDependencyGenerator implements DependencyGenerator
{
    public static class NonZeroExitCodeException extends RuntimeException
    {
        private NonZeroExitCodeException(String command)
        {
            super("`" + command + "` did finish with non-zero exit code");
        }
    }

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
                else {
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


    private void npmInstall(Path repositoryPath) throws Exception
    {
        requireNotYarn(repositoryPath);
        requirePackageLockJson(repositoryPath);

        int exitCode = new ProcessBuilder()
            .command(npmBin, "ci")
            .directory(repositoryPath.toFile())
            .start()
            .waitFor();

        if (exitCode != 0)
        {
            throw new NonZeroExitCodeException(npmBin + " ci");
        }
    }


    private Path buildCycloneDxBom(Path repositoryPath) throws Exception
    {
        int exitCode = new ProcessBuilder()
            .command(npxBin, "@cyclonedx/bom")
            .directory(repositoryPath.toFile())
            .start()
            .waitFor();

        if (exitCode != 0)
        {
            throw new NonZeroExitCodeException(npxBin + " @cyclonedx/bom");
        }

        return repositoryPath.resolve("bom.xml");
    }


    private void requireNotYarn(Path repositoryPath) throws IllegalStateException
    {
        if (Files.exists(repositoryPath.resolve("yarn.lock")))
        {
            throw new YarnNotSupportedException("Found yarn.lock file");
        }
    }


    private void requirePackageLockJson(Path repositoryPath) throws IllegalStateException
    {
        if (Files.notExists(repositoryPath.resolve("package-lock.json")))
        {
            throw new PackageLockJsonMissingException();
        }
    }
}
