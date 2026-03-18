package com.freenow.sauron.plugins.generator.python;

import com.freenow.sauron.plugins.command.NonZeroExitCodeException;
import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PythonDependencyGenerator extends DependencyGenerator
{

    protected String python = "python";
    protected String poetry = "poetry";
    protected String cyclonedxPy = "cyclonedx-py";


    protected PythonDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
        properties.getPluginConfigurationProperty("dependency-checker", "python")
            .ifPresent(pythonConfig ->
            {
                if (pythonConfig instanceof Map)
                {
                    Map<String, Object> config = (Map<String, Object>) pythonConfig;
                    this.python = (String) config.getOrDefault("path", python);
                    this.poetry = (String) config.getOrDefault("poetry", poetry);
                    this.cyclonedxPy = (String) config.getOrDefault("cyclonedx-py", cyclonedxPy);
                }
                else
                {
                    log.warn("Config sauron.plugins.dependency-checker.python is malformed, expected map.");
                }
            });
    }


    @Override
    public Path generateCycloneDxBom(Path repositoryPath)
    {
        try
        {
            generateRequirementsFreeze(repositoryPath);
            Path resolved = repositoryPath.resolve("bom.xml");
            if (resolved.toFile().exists() && Files.size(resolved) > 0)
            {
                log.info("BOM file {} created.", resolved.toAbsolutePath());
            }
            else
            {
                log.info("BOM file is either empty or does not exist.");
            }
            return resolved;
        }
        catch (Exception e)
        {
            log.error("Skip building Python Cyclone DX BOM: {}", e.getMessage());
        }
        return null;
    }


    protected abstract void generateRequirementsFreeze(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException;

    protected String getVenvCreateCommand()
    {
        return python + " -m venv .";
    }


    protected String getVenvActivateCommand()
    {
        return "source bin/activate";
    }


    protected String getVenvDeactivateCommand()
    {
        return "deactivate";
    }
}
