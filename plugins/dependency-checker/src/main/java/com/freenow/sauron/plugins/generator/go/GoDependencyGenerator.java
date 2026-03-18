package com.freenow.sauron.plugins.generator.go;

import com.freenow.sauron.plugins.ProjectType;
import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoDependencyGenerator extends DependencyGenerator
{
    public static final Logger log = LoggerFactory.getLogger(GoDependencyGenerator.class);
    private static final String BOM_XML = "bom.xml";
    private String syftBin = "syft";


    public GoDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
        properties.getPluginConfigurationProperty("dependency-checker", "go")
            .ifPresent(goConfig ->
            {
                if (goConfig instanceof Map)
                {
                    java.util.Map<String, Object> config = (Map<String, Object>) goConfig;
                    this.syftBin = (String) config.getOrDefault("syft", syftBin);
                }
            });
    }


    @Override
    public Path generateCycloneDxBom(Path repositoryPath)
    {
        try
        {
            Path projectPath = checkGoMod(repositoryPath);
            return buildSBOM(projectPath);
        }
        catch (Exception e)
        {
            log.error("Failed to generate Go Cyclone DX BOM: {}", e.getMessage(), e);
        }
        return null;
    }


    private Path buildSBOM(Path projectPath) throws Exception
    {
        Path bomFile = projectPath.resolve(BOM_XML);
        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(projectPath)
            .commandline(java.util.List.of(
                syftBin,
                "scan",
                "file:go.mod",
                "-o",
                "cyclonedx-xml=" + BOM_XML
            ))
            .build()
            .run();

        if (Files.exists(bomFile) && Files.size(bomFile) > 0)
        {
            log.info("Go BOM file {} created.", bomFile.toAbsolutePath());
            return bomFile;
        }

        log.warn("Go BOM file was not created or is empty: {}", bomFile);
        return null;
    }


    private Path checkGoMod(Path repositoryPath) throws IOException
    {
        return ProjectType.findGoMod(repositoryPath)
            .orElseThrow(() -> new GoModMissingException(repositoryPath));
    }


    private static class GoModMissingException extends IllegalStateException
    {
        private GoModMissingException(Path repositoryPath)
        {
            super("go.mod file not found in " + repositoryPath);
        }
    }
}
