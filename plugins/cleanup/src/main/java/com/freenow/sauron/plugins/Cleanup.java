package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
public class Cleanup implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.getStringAdditionalInformation("repositoryPath")
            .filter(repositoryPath -> !repositoryPath.isBlank())
            .ifPresent(repositoryPath ->
            {
                try
                {
                    Files.walk(Path.of(repositoryPath))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                }
            });

        return input;
    }
}