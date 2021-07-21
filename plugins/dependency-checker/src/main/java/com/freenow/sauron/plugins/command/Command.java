package com.freenow.sauron.plugins.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import lombok.Builder;

@Builder
public class Command
{

    private final Map<String, String> environment;
    private final File outputFile;
    private final String commandline;
    private final Path repositoryPath;


    public void run() throws IOException, InterruptedException, NonZeroExitCodeException
    {
        ProcessBuilder builder = new ProcessBuilder()
            .command(commandline.split("\\s"))
            .directory(repositoryPath.toFile());

        if (environment != null && !environment.isEmpty())
        {
            builder.environment().putAll(environment);
        }
        Process process = builder.start();

        if (process.waitFor() != 0)
        {
            throw new NonZeroExitCodeException(commandline, new String(process.getErrorStream().readAllBytes()));
        }

        if (outputFile != null)
        {
            try (OutputStream output = new FileOutputStream(outputFile))
            {
                process.getInputStream().transferTo(output);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
    }
}
