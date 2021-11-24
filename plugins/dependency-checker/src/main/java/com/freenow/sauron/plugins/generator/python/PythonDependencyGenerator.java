package com.freenow.sauron.plugins.generator.python;

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
public abstract class PythonDependencyGenerator extends DependencyGenerator
{
    protected static final String ENV_PATH = "env";
    protected static final String REQUIREMENTS_FREEZE_FILE = "requirements.freeze";

    private static final String PIP_INSTALL_CYCLONE_DX_COMMAND = "-m pip install --target env cyclonedx-bom";
    private static final String CYCLONE_DX_PY_COMMAND = "env/cyclonedx_py/client.py -r -i requirements.freeze -o bom.xml";

    public static class RequirementsFreezeMissingException extends IllegalStateException
    {
        private RequirementsFreezeMissingException()
        {
            super("Project is missing requirements.freeze");
        }
    }

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
            requireRequirementsFreeze(repositoryPath);
            return buildCycloneDxBom(repositoryPath);
        }
        catch (Exception e)
        {
            log.error("Skip building Python Cyclone DX BOM: {}", e.getMessage());
        }

        return null;
    }


    protected abstract void generateRequirementsFreeze(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException;


    private Path buildCycloneDxBom(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException
    {
        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(PIP_INSTALL_CYCLONE_DX_COMMAND))
            .build()
            .run();

        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(CYCLONE_DX_PY_COMMAND))
            .environment(Map.of("PYTHONPATH", repositoryPath.resolve(ENV_PATH).toString()))
            .build()
            .run();

        return repositoryPath.resolve("bom.xml");
    }


    protected String pythonCommand(String parameters)
    {
        return String.join(" ", python, parameters);
    }


    private void requireRequirementsFreeze(Path repositoryPath) throws RequirementsFreezeMissingException
    {
        if (Files.notExists(repositoryPath.resolve(REQUIREMENTS_FREEZE_FILE)))
        {
            throw new RequirementsFreezeMissingException();
        }
    }
}
