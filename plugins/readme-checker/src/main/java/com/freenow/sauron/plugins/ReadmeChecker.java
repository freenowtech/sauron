package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
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
    private static final boolean CONFIG_DEFAULT_CASE_SENSITIVE = false;


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.getStringAdditionalInformation("repositoryPath").ifPresent(repositoryPath ->
        {
            try
            {
                String minLength = String.valueOf(properties.getPluginConfigurationProperty("readme-checker", "minLength").orElse(CONFIG_DEFAULT_MIN_SIZE));
                Boolean caseSensitive = Boolean.valueOf(String.valueOf(properties.getPluginConfigurationProperty("readme-checker", "caseSensitive")
                    .orElse(CONFIG_DEFAULT_CASE_SENSITIVE)));
                long size = DataSize.parse(minLength).toBytes();

                Optional<String> readmeFilename = getReadmeFilename(Paths.get(repositoryPath), caseSensitive);
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


    private Optional<String> getReadmeFilename(Path repositoryPath, Boolean caseSensitive)
    {
        Stream<String> markdownFiles = Arrays.stream(repositoryPath.toFile().list((dir, name) -> name.contains(".md")));
        if (caseSensitive)
        {
            return markdownFiles.filter(s -> s.equals("README.md")).findFirst();
        }
        else
        {
            return markdownFiles.filter(s -> s.equalsIgnoreCase("readme.md")).findFirst();
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
