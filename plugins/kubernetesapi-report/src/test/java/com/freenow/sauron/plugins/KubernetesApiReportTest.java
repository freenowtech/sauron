package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesApiReportTest
{
    @Spy
    private ApiClient client;

    @InjectMocks
    private KubernetesApiReport plugin;


    @Before
    public void init() throws NoSuchFieldException
    {
        FieldSetter.setField(plugin, plugin.getClass().getDeclaredField("client"), client);
    }


    @Test
    public void testKubernetesApiReportEmptyAnnotationsLabelsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of(), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of(
            "0", "test/label"
        ));
        plugin.apply(properties, dataSet);
        checkKeyNotPresent(dataSet, "test/label");
    }


    @Test
    public void testKubernetesApiReportHaveAnnotationsEmptyLabelsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of("test/annotation", "myservice"), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of(
            "0", "test/annotation"
        ));
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, "test/annotation", "myservice");
    }


    @Test
    public void testKubernetesApiReportEmptyAnnotationHaveLabelsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of(), Map.of("test/label", "myservice")));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of(
            "0", "test/label"
        ));
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, "test/label", "myservice");
    }


    @Test
    public void testKubernetesApiReportWrongLabelsAnnotationsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of("test/annotation", "myservice"), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of(
            "0", "test/wronglabel"
        ));
        plugin.apply(properties, dataSet);
        checkKeyNotPresent(dataSet, "test/wronglabel");
    }


    @Test
    public void testKubernetesApiReportBothLabelsAnnotationsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of("test/annotation", "myserviceannotation"), Map.of("test/annotation", "myservicelabel")));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of(
            "0", "test/annotation"
        ));
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, "test/annotation", "myserviceannotation");
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
        dataSet.setServiceName("myservice");
        return dataSet;
    }


    private PluginsConfigurationProperties createPluginConfigurationProperties(Map<String, String> selectors)
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put("kubernetesapi-report", Map.of(
            "serviceLabel", "serviceLabel",
            "selectors", Map.of("deployment", selectors))
        );
        return properties;
    }


    @SneakyThrows
    private void mockV1DeploymentListApiResponse(V1DeploymentList list)
    {
        ApiResponse<V1DeploymentList> api = new ApiResponse<>(200, null, list);
        doReturn(api).when(client).execute(any(), eq(new TypeToken<V1DeploymentList>()
        {
        }.getType()));
    }


    private V1DeploymentList createDeploymentList(Map<String, String> annotations, Map<String, String> labels)
    {
        return new V1DeploymentList().items(List.of(new V1Deployment().metadata(createObjectMeta(annotations, labels))));
    }


    private V1ObjectMeta createObjectMeta(Map<String, String> annotations, Map<String, String> labels)
    {
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setAnnotations(annotations);
        objectMeta.setLabels(labels);
        return objectMeta;
    }
}
