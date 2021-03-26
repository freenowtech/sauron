package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Test;
import org.springframework.util.StringUtils;

public class ReadmeCheckerTest
{

    private static final String MISSING_OR_EMPTY_README = "missingOrEmptyReadme";


    @Test
    public void testReadmeCheckerNoConfig()
    {
        ReadmeChecker plugin = new ReadmeChecker();
        DataSet dataSet = plugin.apply(new PluginsConfigurationProperties(), new DataSet());
        checkKeyNotPresent(dataSet, MISSING_OR_EMPTY_README);
    }


    @Test
    public void testReadmeCheckerNoReadme() throws IOException
    {
        DataSet dataSet = apply(-1, null);
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, true);
    }


    @Test
    public void testReadmeCheckerNoReadme1BConfig() throws IOException
    {
        DataSet dataSet = apply(-1, "1B");
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, true);
    }


    @Test
    public void testReadmeCheckerEmptyReadmeNoConfig() throws IOException
    {
        DataSet dataSet = apply(0, null);
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, true);
    }


    @Test
    public void testReadmeCheckerEmptyReadme1BConfig() throws IOException
    {
        DataSet dataSet = apply(0, "1B");
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, true);
    }


    @Test
    public void testReadmeCheckerSmallReadme() throws IOException
    {
        DataSet dataSet = apply(1, "2B");
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, true);
    }


    @Test
    public void testReadmeCheckerEqualsLengthReadme() throws IOException
    {
        DataSet dataSet = apply(2, "2B");
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, false);
    }


    @Test
    public void testReadmeCheckerBigReadme() throws IOException
    {
        DataSet dataSet = apply(10, "1B");
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, false);
    }


    @Test
    public void testReadmeCheckerReadmeNoConfig() throws IOException
    {
        DataSet dataSet = apply(1, null);
        checkKeyPresent(dataSet, MISSING_OR_EMPTY_README, false);
    }


    private DataSet apply(int readmeSize, String minLength) throws IOException
    {
        ReadmeChecker plugin = new ReadmeChecker();
        DataSet dataSet = createDataSet(readmeSize);
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(minLength);
        return plugin.apply(properties, dataSet);
    }


    private void checkKeyPresent(DataSet dataSet, String key, Object expected)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assert (response.isPresent() && response.get().equals(expected));
    }


    private void checkKeyNotPresent(DataSet dataSet, String key)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assert (!response.isPresent());
    }


    private PluginsConfigurationProperties createPluginConfigurationProperties(String minLength)
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        if (!StringUtils.isEmpty(minLength))
        {
            properties.put("readme-checker", new HashMap<String, Object>()
            {{
                put("minLength", minLength);
            }});
        }
        return properties;
    }


    private DataSet createDataSet(int readmeSize) throws IOException
    {
        Path repositoryPath = Files.createTempDirectory("readme-checker-test");
        repositoryPath.toFile().deleteOnExit();

        if (readmeSize > -1)
        {
            try (FileOutputStream stream = new FileOutputStream(repositoryPath.resolve("README.md").toFile()))
            {
                stream.write(new byte[readmeSize]);
            }
        }

        DataSet dataSet = new DataSet();
        dataSet.setAdditionalInformation("repositoryPath", repositoryPath.toString());
        return dataSet;
    }
}
