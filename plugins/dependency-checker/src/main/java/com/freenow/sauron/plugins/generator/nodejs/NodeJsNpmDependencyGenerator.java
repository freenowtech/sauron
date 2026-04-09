package com.freenow.sauron.plugins.generator.nodejs;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NodeJsNpmDependencyGenerator extends NodeJsDependencyGenerator {
    private static final String BOM_JSON = "bom.json";
    private static final String[] NPM_SBOM = {
            "sbom",
            "--omit=dev",
            "--sbom-format=cyclonedx",
            "--package-lock-only",
            "--legacy-peer-deps",
    };

    private String npmBin = "npm";


    public NodeJsNpmDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties, "package-lock.json");

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
    protected Path buildCycloneDxBom(Path repositoryPath) throws IOException, InterruptedException
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

}
