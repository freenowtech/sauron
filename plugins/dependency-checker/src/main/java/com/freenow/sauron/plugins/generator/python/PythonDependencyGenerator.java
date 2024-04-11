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

    protected static final String PYTHON_VIRTUAL_ENV_CREATE = "python -m venv .";
    protected static final String PYTHON_VIRTUAL_ENV_ACTIVATE = "source bin/activate";
    protected static final String CYCLONE_DX_GENERATE_BOM = "cyclonedx-py requirements requirements.freeze --of XML -o bom.xml";
    protected static final String PYTHON_VIRTUAL_ENV_DEACTIVATE = "deactivate";
    protected String python = "python";


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

}
