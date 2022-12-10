package com.freenow.sauron.plugins.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Builder
public class Command
{

    private final Map<String, String> environment;
    private final File outputFile;
    private final List<String> commandline;
    private final Path repositoryPath;
    private final Integer commandTimeout;


    public void run() throws IOException, InterruptedException, NonZeroExitCodeException
    {
        ProcessBuilder builder = new ProcessBuilder()
            .command(commandline.get(0), commandline.get(1))
            .directory(repositoryPath.toFile());

        if (environment != null && !environment.isEmpty())
        {
            builder.environment().putAll(environment);
        }
        log.info(builder.command().toString());
        Process process = builder.start();

        if (!process.waitFor(commandTimeout, TimeUnit.MINUTES))
        {
            throw new NonZeroExitCodeException(commandline.toString(), new String(process.getErrorStream().readAllBytes()));
        }

        String processLogs = new String(process.getErrorStream().readAllBytes());
        if (isNotBlank(processLogs))
        {
            log.info(processLogs);
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
