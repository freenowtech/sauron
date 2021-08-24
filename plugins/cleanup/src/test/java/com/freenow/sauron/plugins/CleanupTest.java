package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class CleanupTest
{
    private static final String DIRECTORY_NAME = "toBeDeleted";


    @Test
    public void testCleanupExistingDirectory() throws IOException
    {
        Path pathToBeDeleted = Files.createTempDirectory(DIRECTORY_NAME);
        new Cleanup().apply(new PluginsConfigurationProperties(), createDataSet(pathToBeDeleted));
        assertFalse("Directory still exists", Files.exists(pathToBeDeleted));
    }


    @Test
    public void testCleanupNonExistingDirectory()
    {
        Path pathToBeDeleted = Path.of(DIRECTORY_NAME);
        new Cleanup().apply(new PluginsConfigurationProperties(), createDataSet(pathToBeDeleted));
        assertFalse("Directory still exists", Files.exists(pathToBeDeleted));
    }


    private DataSet createDataSet(Path repositoryPath)
    {
        DataSet dataSet = new DataSet();
        dataSet.setAdditionalInformation("repositoryPath", repositoryPath.toString());
        return dataSet;
    }
}
