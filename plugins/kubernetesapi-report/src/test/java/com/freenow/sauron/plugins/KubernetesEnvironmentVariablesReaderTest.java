package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.readers.KubernetesEnvironmentVariablesReader;
import com.freenow.sauron.plugins.readers.KubernetesLabelAnnotationReader;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.KubernetesApiReport.ENV_VARS_PROPERTY;
import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static com.freenow.sauron.plugins.KubernetesApiReport.SERVICE_LABEL_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesEnvironmentVariablesReaderTest
{
    private static final String SERVICE_LABEL = "mytaxi/service.name";

    private static final String SERVICE_NAME = "serviceName";

    private static final String UNKNOWN_SERVICE_NAME = "unknown";

    private static final String ENV_VAR = "ENV_VAR";

    @Spy
    private ApiClient client;

    @Mock
    private KubernetesExecCommand execCommand;

    private KubernetesApiReport plugin;


    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        plugin = new KubernetesApiReport(
            new KubernetesLabelAnnotationReader(client),
            new KubernetesEnvironmentVariablesReader(new KubernetesGetObjectMetaCommand(client), execCommand));
    }


    @Test
    public void testKubernetesEnvironmentVariablesReaderUnknownService()
    {
        mockV1PodListApiResponse(new V1PodList());

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties();
        plugin.apply(properties, dataSet);
        checkKeyNotPresent(dataSet, ENV_VAR);
    }


    @Test
    public void testKubernetesEnvironmentVariablesReaderEnvVarNotExists()
    {
        mockV1PodListApiResponse(createPodList());
        mockExecCommand(Optional.empty());

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties();
        plugin.apply(properties, dataSet);
        checkKeyNotPresent(dataSet, ENV_VAR);
    }


    @Test
    public void testKubernetesEnvironmentVariablesReaderEnvVarExists()
    {
        mockV1PodListApiResponse(createPodList());
        mockExecCommand(Optional.of(String.format("%s=test", ENV_VAR)));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties();
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, ENV_VAR, "test");
    }


    private void checkKeyPresent(DataSet dataSet, String key, Object expected)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }


    private void checkKeyNotPresent(DataSet dataSet, String key)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assert (response.isEmpty());
    }


    private DataSet createDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName(UNKNOWN_SERVICE_NAME);
        return dataSet;
    }


    private PluginsConfigurationProperties createPluginConfigurationProperties()
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put(PLUGIN_ID, Map.of(
            SERVICE_LABEL_PROPERTY, SERVICE_LABEL,
            ENV_VARS_PROPERTY, Map.of("0", ENV_VAR))
        );
        return properties;
    }


    @SneakyThrows
    private void mockV1PodListApiResponse(V1PodList list)
    {
        ApiResponse<V1PodList> api = new ApiResponse<>(200, null, list);
        doReturn(api).when(client).execute(any(), eq(new TypeToken<V1PodList>()
        {
        }.getType()));
    }


    @SneakyThrows
    private void mockExecCommand(Optional<String> envs)
    {
        doReturn(envs).when(execCommand).exec(anyString(), any());
    }


    private V1PodList createPodList()
    {
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setLabels(Map.of(SERVICE_LABEL, SERVICE_NAME));
        objectMeta.setName(SERVICE_NAME);
        return new V1PodList().items(List.of(new V1Pod().metadata(objectMeta)));
    }
}
