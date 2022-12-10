package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import java.util.Map;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class APIClientFactoryTest
{
    private static final String DEFAULT = "default";
    private static final String CLUSTER_A = "cluster-a";
    private static final String CLUSTER_B = "cluster-b";
    private static final String KUBERNETES_CLUSTER_DEFAULT = "http://localhost";
    public static final String KUBERNETES_CLUSTER_A_COM = "https://kubernetes.cluster-a.com";
    public static final String KUBERNETES_CLUSTER_B_COM = "https://kubernetes.cluster-b.com";
    private APIClientFactory apiClientFactory = new APIClientFactory();


    @Test
    public void defaultApiClient()
    {
        try (MockedStatic<Config> config = Mockito.mockStatic(Config.class))
        {
            config.when(Config::defaultClient).thenReturn(new ApiClient().setBasePath(KUBERNETES_CLUSTER_DEFAULT));
            config.when(() -> Config.fromUrl(KUBERNETES_CLUSTER_A_COM)).thenReturn(new ApiClient().setBasePath(KUBERNETES_CLUSTER_A_COM));
            config.when(() -> Config.fromUrl(KUBERNETES_CLUSTER_B_COM)).thenReturn(new ApiClient().setBasePath(KUBERNETES_CLUSTER_B_COM));

            final var apiClient = apiClientFactory.get(dummyDataSet(DEFAULT), dummyPluginConfig());
            assertNotNull(apiClient);
            assertTrue(apiClient.getBasePath().contains(KUBERNETES_CLUSTER_DEFAULT));
            assertFalse(apiClient.getBasePath().contains(CLUSTER_B));
            assertFalse(apiClient.getBasePath().contains(CLUSTER_A));
        }
    }


    @Test
    public void clusterBApiClient()
    {
        try (MockedStatic<Config> config = Mockito.mockStatic(Config.class))
        {
            config.when(Config::defaultClient).thenReturn(new ApiClient().setBasePath(KUBERNETES_CLUSTER_DEFAULT));
            config.when(() -> Config.fromUrl(KUBERNETES_CLUSTER_A_COM)).thenReturn(new ApiClient().setBasePath(KUBERNETES_CLUSTER_A_COM));
            config.when(() -> Config.fromUrl(KUBERNETES_CLUSTER_B_COM)).thenReturn(new ApiClient().setBasePath(KUBERNETES_CLUSTER_B_COM));

            final var apiClient = apiClientFactory.get(dummyDataSet(CLUSTER_B), dummyPluginConfig());
            assertNotNull(apiClient);
            assertTrue(apiClient.getBasePath().contains(CLUSTER_B));
            assertFalse(apiClient.getBasePath().contains(KUBERNETES_CLUSTER_DEFAULT));
            assertFalse(apiClient.getBasePath().contains(CLUSTER_A));
        }
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
                    CLUSTER_A, KUBERNETES_CLUSTER_A_COM,
                    CLUSTER_B, KUBERNETES_CLUSTER_B_COM
                )
            )
        );
        return properties;
    }
}