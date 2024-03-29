package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.util.unit.DataSize;

@Slf4j
@Extension
public class ReadmeChecker implements SauronExtension
{
    private static final String CONTENT_CHECK_1 = "Let people know what your project can do specifically. Provide context and add a link to any reference the target audience might be unfamiliar with.";

    private static final String CONTENT_CHECK_2 = "The team/tribe/organizational unit responsible for this service and at least one fast and good way how to contact them, e.g. slack channel.";

    private static final String CONFIG_DEFAULT_MIN_SIZE = "1B";


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.getStringAdditionalInformation("repositoryPath").ifPresent(repositoryPath ->
        {
            try
            {
                String minLength = String.valueOf(properties.getPluginConfigurationProperty("readme-checker", "minLength").orElse(CONFIG_DEFAULT_MIN_SIZE));
                long size = DataSize.parse(minLength).toBytes();

                Optional<String> readmeFilename = getReadmeFilename(Paths.get(repositoryPath));
                boolean missingOrEmptyReadme = readmeFilename.isEmpty();
                if (!missingOrEmptyReadme)
                {
                    Path readmeFile = Paths.get(repositoryPath, readmeFilename.get());
                    missingOrEmptyReadme = (Files.size(readmeFile) < size);
                    if (!missingOrEmptyReadme)
                    {
                        missingOrEmptyReadme = badFileContent(readmeFile);
                    }
                }

                input.setAdditionalInformation("missingOrEmptyReadme", missingOrEmptyReadme);

            }
            catch (IOException ex)
            {
                log.error(ex.getMessage(), ex);
            }
        });

        return input;
    }


    private Optional<String> getReadmeFilename(Path repositoryPath)
    {
        try
        {
            return Files.list(repositoryPath)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name ->
                    name.equalsIgnoreCase("readme.md") || name.equalsIgnoreCase("readme.rst")
                )
                .findFirst();
        }
        catch (IOException e)
        {
            return Optional.empty();
        }
    }


    private boolean badFileContent(Path path) throws IOException
    {
        InputStream is = new FileInputStream(path.toFile());
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));

        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
        while (line != null)
        {
            sb.append(line).append("\n");
            line = buf.readLine();
        }

        String content = sb.toString();

        return
            content.contains(CONTENT_CHECK_1)
                || content.contains(CONTENT_CHECK_2);

    }

}
