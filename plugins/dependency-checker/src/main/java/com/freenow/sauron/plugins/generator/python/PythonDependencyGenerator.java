package com.freenow.sauron.plugins.generator.python;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.plugins.command.NonZeroExitCodeException;
import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PythonDependencyGenerator extends DependencyGenerator
{
    protected static final String REQUIREMENTS_FREEZE_FILE = "requirements.freeze";
    protected static final String PIP_INSTALL_CYCLONE_DX_BOM = "python -m pip install --target env cyclonedx-bom";
    protected static final String CYCLONE_DX_GENERATE_BOM = "python -m cyclonedx_py -r -i ../" + REQUIREMENTS_FREEZE_FILE + " -o ../bom.xml";
    protected static final String PYTHON_VIRTUAL_ENV_CREATE = "-m venv .";
    protected static final String PYTHON_VIRTUAL_ENV_ACTIVATE = "source bin/activate";
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
            createPythonVirtualEnv(repositoryPath);
            generateRequirementsFreeze(repositoryPath);

            return repositoryPath.resolve("bom.xml");
        }
        catch (Exception e)
        {
            log.error("Skip building Python Cyclone DX BOM: {}", e.getMessage());
        }

        return null;
    }


    private void createPythonVirtualEnv(Path repositoryPath) throws IOException, InterruptedException
    {
        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(PYTHON_VIRTUAL_ENV_CREATE))
            .build()
            .run();
    }


    protected abstract void generateRequirementsFreeze(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException;


    protected List<String> pythonCommand(String parameters)
    {
        return List.of(python, parameters);
    }
}
