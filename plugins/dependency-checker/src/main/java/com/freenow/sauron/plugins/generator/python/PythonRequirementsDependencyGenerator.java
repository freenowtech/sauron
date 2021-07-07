package com.freenow.sauron.plugins.generator.python;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PythonRequirementsDependencyGenerator extends PythonDependencyGenerator
{
    private static final String PIP_INSTALL_COMMAND = "-m pip install -r requirements.txt --target env";
    private static final String FREEZE_COMMAND = "-m pip freeze --path env";


    public PythonRequirementsDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
    }


    @Override
    protected void generateRequirementsFreeze(Path repositoryPath)
    {
        try
        {
            pipInstall(repositoryPath);
            pipFreeze(repositoryPath);
        }
        catch (IllegalStateException | IOException | InterruptedException e)
        {
            log.error("Failing generating requirements freeze: {}", e.getMessage());
        }
    }


    private void pipInstall(Path repositoryPath) throws IOException, InterruptedException
    {
        Command.builder()
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(PIP_INSTALL_COMMAND))
            .build()
            .run();
    }


    private void pipFreeze(Path repositoryPath) throws IOException, InterruptedException
    {
        Command.builder()
            .repositoryPath(repositoryPath)
            .commandline(pythonCommand(FREEZE_COMMAND))
            .outputFile(repositoryPath.resolve(REQUIREMENTS_FREEZE_FILE).toFile())
            .build()
            .run();
    }
}
