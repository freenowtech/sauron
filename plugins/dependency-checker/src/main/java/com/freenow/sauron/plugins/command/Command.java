package com.freenow.sauron.plugins.command;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Builder
public class Command
{
    public static final String BIN_BASH = "/bin/bash";
    public static final String BASH_C_OPTION = "-c";
    public static final String AND = " && ";
    private final Map<String, String> environment;
    private final Path outputFile;
    private final List<String> commandline;
    private final Path repositoryPath;
    private final Integer commandTimeout;


    public void run() throws IOException, InterruptedException, NonZeroExitCodeException
    {
        ProcessBuilder builder = new ProcessBuilder()
            .command(commandline)
            .directory(repositoryPath.toFile());

        if (environment != null && !environment.isEmpty())
        {
            builder.environment().putAll(environment);
        }
        log.info(builder.command().toString());
        if (outputFile != null)
        {
            builder.redirectOutput(outputFile.toFile());
        }
        Process process = builder.start();

        if (!process.waitFor(commandTimeout, TimeUnit.MINUTES))
        {
            throw new NonZeroExitCodeException(commandline.toString(), new String(process.getErrorStream().readAllBytes()));
        }

        InputStream processStdOut = process.getInputStream();
        if (processStdOut != null)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processStdOut)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    log.debug(line);
                }
            }
        }

        String processLogs = new String(process.getErrorStream().readAllBytes());
        if (isNotBlank(processLogs))
        {
            log.info(processLogs);
        }
    }
}
