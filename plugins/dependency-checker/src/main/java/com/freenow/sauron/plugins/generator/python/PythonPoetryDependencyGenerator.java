package com.freenow.sauron.plugins.generator.python;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.plugins.command.NonZeroExitCodeException;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PythonPoetryDependencyGenerator extends PythonDependencyGenerator
{
    private static final String PIP_INSTALL_POETRY = "-m pip install --target env poetry==1.1.15";
    private static final String POETRY_EXPORT = "-m poetry export --output requirements.freeze --without-hashes";


    public PythonPoetryDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
    }


    @Override
    protected void generateRequirementsFreeze(Path repositoryPath) throws IOException, InterruptedException, NonZeroExitCodeException
    {
        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(PIP_INSTALL_POETRY))
            .build()
            .run();

        Command.builder()
            .commandTimeout(commandTimeoutMinutes)
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(POETRY_EXPORT))
            .environment(Map.of("PYTHONPATH", repositoryPath.resolve(ENV_PATH).toString()))
            .build()
            .run();
    }
}
