package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DependencytrackPublisherTest
{
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DependencytrackPublisher plugin;


    @Test
    public void testDependencytrackPublisherApplyEmptyEnvironmentsList() throws IOException
    {
        plugin.apply(createPluginConfigurationProperties(Map.of()), createDataSet("live"));
        verify(restTemplate).put(any(), any());
    }


    @Test
    public void testDependencytrackPublisherApplyAllowedEnvironment() throws IOException
    {
        plugin.apply(createPluginConfigurationProperties(Map.of("0", "live")), createDataSet("live"));
        verify(restTemplate).put(any(), any());
    }


    @Test
    public void testDependencytrackPublisherApplyNoEnvironment() throws IOException
    {
        plugin.apply(createPluginConfigurationProperties(Map.of("0", "live")), createDataSet(null));
        verify(restTemplate).put(any(), any());
    }


    @Test
    public void testDependencytrackPublisherApplyEmptyEnvironment() throws IOException
    {
        plugin.apply(createPluginConfigurationProperties(Map.of("0", "live")), createDataSet(""));
        verify(restTemplate).put(any(), any());
    }


    @Test
    public void testDependencytrackPublisherApplyNoEnvironmentEmptyAllowedEnvironments() throws IOException
    {
        plugin.apply(createPluginConfigurationProperties(Map.of()), createDataSet(null));
        verify(restTemplate).put(any(), any());
    }


    @Test
    public void testDependencytrackPublisherApplyDisallowedEnvironment() throws IOException
    {
        plugin.apply(createPluginConfigurationProperties(Map.of("0", "sandbox")), createDataSet("live"));
        verify(restTemplate, never()).put(any(), any());
    }


    private DataSet createDataSet(String environment) throws IOException
    {
        DataSet dataSet = new DataSet();
        if (environment != null)
        {
            dataSet.setAdditionalInformation("environment", environment);
        }
        dataSet.setAdditionalInformation("cycloneDxBomPath", File.createTempFile("dependencytrack", "test").getPath());
        return dataSet;
    }


    private PluginsConfigurationProperties createPluginConfigurationProperties(Map<String, String> environments)
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put("dependencytrack-publisher", Map.of(
            "environments", environments,
            "uri", "https://dependencytrack.test",
            "api-key", "test"
        ));
        return properties;
    }
}