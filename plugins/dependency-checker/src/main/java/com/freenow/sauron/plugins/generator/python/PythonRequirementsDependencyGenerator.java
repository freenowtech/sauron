package com.freenow.sauron.plugins.generator.python;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.command.Command.BASH_C_OPTION;
import static com.freenow.sauron.plugins.command.Command.BIN_BASH;
import static com.freenow.sauron.plugins.command.Command.AND;

@Slf4j
public class PythonRequirementsDependencyGenerator extends PythonDependencyGenerator
{
    private static final String PIP_INSTALL_COMMAND = "python -m pip install -r requirements.txt --target env";
    private static final String FREEZE_COMMAND = "python -m pip freeze --path env > requirements.freeze";


    public PythonRequirementsDependencyGenerator(PluginsConfigurationProperties properties)
    {
        super(properties);
    }


    @Override
    protected void generateRequirementsFreeze(Path repositoryPath)
    {
        try
        {
            Command.builder()
                .commandTimeout(commandTimeoutMinutes)
                .repositoryPath(repositoryPath)
                .commandline(
                    List.of(BIN_BASH, BASH_C_OPTION,
                        PYTHON_VIRTUAL_ENV_ACTIVATE + AND +
                        PIP_INSTALL_COMMAND + AND +
                        FREEZE_COMMAND + AND +
                        PIP_INSTALL_CYCLONE_DX_BOM + AND +
                        GO_TO_ENV + AND + CYCLONE_DX_GENERATE_BOM + AND +
                        RETURN + AND +
                        PYTHON_VIRTUAL_ENV_DEACTIVATE
                    )
                )
                .build()
                .run();
        }
        catch (IllegalStateException | IOException | InterruptedException e)
        {
            log.error("Failing generating requirements freeze: {}", e.getMessage());
        }
    }
}
