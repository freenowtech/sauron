package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class APIClientFactoryTest
{
    private static final String DEFAULT = "default";
    private static final String CLUSTER_A = "cluster-a";
    private static final String CLUSTER_B = "cluster-b";
    private APIClientFactory apiClientFactory = new APIClientFactory();


    @Test
    public void defaultApiClient()
    {
        final var apiClient = apiClientFactory.get(dummyDataSet(DEFAULT), dummyPluginConfig());
        assertNotNull(apiClient);
        assertFalse(apiClient.getBasePath().contains(CLUSTER_B));
        assertFalse(apiClient.getBasePath().contains(CLUSTER_A));
    }


    @Test
    public void clusterBApiClient()
    {
        final var apiClient = apiClientFactory.get(dummyDataSet(CLUSTER_A), dummyPluginConfig());
        assertNotNull(apiClient);
        assertTrue(apiClient.getBasePath().contains(CLUSTER_A));
        assertFalse(apiClient.getBasePath().contains(CLUSTER_B));
    }


    @Test
    public void useExistentClusterAApiClient()
    {
        apiClientFactory = new APIClientFactory(new HashMap<>());
        final var apiClient = apiClientFactory.get(dummyDataSet(CLUSTER_B), dummyPluginConfig());
        assertNotNull(apiClient);
        assertTrue(apiClient.getBasePath().contains(CLUSTER_B));
    }


    private DataSet dummyDataSet(final String environment)
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName("");
        dataSet.setAdditionalInformation("environment", environment);
        return dataSet;
    }


    private PluginsConfigurationProperties dummyPluginConfig()
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put(
            PLUGIN_ID,
            Map.of(
                "apiClientConfig", Map.of(
                    DEFAULT, "",
                    CLUSTER_A, "https://kubernetes.cluster-a.com",
                    CLUSTER_B, "https://kubernetes.cluster-b.com"
                )
            )
        );
        return properties;
    }
}
