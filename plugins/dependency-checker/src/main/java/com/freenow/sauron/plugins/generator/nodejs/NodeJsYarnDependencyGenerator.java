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
public class NodeJsYarnDependencyGenerator extends NodeJsDependencyGenerator {
    private static final String BOM_JSON = "bom.json";
    private static final String[] YARN_CYCLONEDX_SBOM = {
            "yarn",
            "cyclonedx",
            "--package-lock-only",
            "--prod",
            "--short-PURLs",
            "--output-file",
            BOM_JSON,
    };

    private String corepack = "corepack";


    public NodeJsYarnDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties, "yarn.lock");

        properties.getPluginConfigurationProperty("dependency-checker", "nodejs")
            .ifPresent(nodeJsConfig ->
            {
                if (nodeJsConfig instanceof Map)
                {
                    Map<String, Object> config = (Map<String, Object>) nodeJsConfig;
                    this.corepack = (String) config.getOrDefault("corepack", corepack);
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
        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(
                Stream.concat(
                    Stream.of(corepack),
                    Arrays.stream(YARN_CYCLONEDX_SBOM)
                )
                    .collect(Collectors.toList())
            )
            .build()
            .run();
        return repositoryPath.resolve(BOM_JSON);
    }

}
