package com.freenow.sauron.plugins.generator.nodejs;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.plugins.command.NonZeroExitCodeException;
import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NodeJsDependencyGenerator extends DependencyGenerator
{
    private static final String BOM_JSON = "bom.json";
    private static final String[] NPM_SBOM = {
            "sbom",
            "--omit=dev",
            "--sbom-format=cyclonedx",
            "--package-lock-only",
            "--legacy-peer-deps",
    };

    private static class PackageLockJsonMissingException extends IllegalStateException
    {
        private PackageLockJsonMissingException()
        {
            super("Project is missing package-lock.json");
        }
    }

    private static class PackageManagerNotSupportedException extends IllegalStateException
    {
        private PackageManagerNotSupportedException(String packageManager, String message)
        {
            super(packageManager + " is not supported: " + message);
        }
    }

    private String npmBin = "npm";


    public NodeJsDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);

        properties.getPluginConfigurationProperty("dependency-checker", "nodejs")
            .ifPresent(nodeJsConfig ->
            {
                if (nodeJsConfig instanceof Map)
                {
                    Map<String, Object> config = (Map<String, Object>) nodeJsConfig;
                    this.npmBin = (String) config.getOrDefault("npm", npmBin);
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
            checkPackageManager(repositoryPath);
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


    private void checkPackageManager(Path repositoryPath) throws PackageManagerNotSupportedException, PackageLockJsonMissingException
    {
        requireNotYarn(repositoryPath);
        requireNotPnpm(repositoryPath);
        requirePackageLockJson(repositoryPath);
    }


    private Path buildCycloneDxBom(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException
    {
        Path bomJson = repositoryPath.resolve(BOM_JSON);
        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(
                Stream.concat(
                    Stream.of(npmBin),
                    Arrays.stream(NPM_SBOM)
                )
                    .collect(Collectors.toList())
            )
            .outputFile(bomJson)
            .build()
            .run();
        return bomJson;
    }


    private void requireNotYarn(Path repositoryPath) throws PackageManagerNotSupportedException
    {
        if (Files.exists(repositoryPath.resolve("yarn.lock")))
        {
            throw new PackageManagerNotSupportedException("yarn", "Found yarn.lock file");
        }
    }


    private void requireNotPnpm(Path repositoryPath) throws PackageManagerNotSupportedException
    {
        if (Files.exists(repositoryPath.resolve("pnpm-lock.yaml")))
        {
            throw new PackageManagerNotSupportedException("pnpm", "Found pnpm-lock.yaml file");
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
