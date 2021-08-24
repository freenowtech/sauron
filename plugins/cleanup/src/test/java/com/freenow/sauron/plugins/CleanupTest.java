package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class CleanupTest
{
    private static final String DIRECTORY_NAME = "toBeDeleted";


    @Test
    public void testCleanupExistingDirectory() throws IOException
    {
        Path pathToBeDeleted = Files.createTempDirectory(DIRECTORY_NAME);
        new Cleanup().apply(new PluginsConfigurationProperties(), createDataSet(pathToBeDeleted.toString()));
        assertFalse("Directory still exists", Files.exists(pathToBeDeleted));
    }


    @Test
    public void testCleanupNonExistingDirectory()
    {
        Path pathToBeDeleted = Path.of(DIRECTORY_NAME);
        new Cleanup().apply(new PluginsConfigurationProperties(), createDataSet(pathToBeDeleted.toString()));
        assertFalse("Directory still exists", Files.exists(pathToBeDeleted));
    }


    @Test
    public void testCleanupNullDirectory()
    {
        assertNotNull(new Cleanup().apply(new PluginsConfigurationProperties(), new DataSet()));
    }


    @Test
    public void testCleanupEmptyDirectory()
    {
        assertNotNull(new Cleanup().apply(new PluginsConfigurationProperties(), createDataSet("")));
    }


    private DataSet createDataSet(String repositoryPath)
    {
        DataSet dataSet = new DataSet();
        dataSet.setAdditionalInformation("repositoryPath", repositoryPath);
        return dataSet;
    }
}
