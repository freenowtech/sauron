package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import io.kubernetes.client.openapi.models.V1beta1CronJobList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static com.freenow.sauron.plugins.KubernetesApiReport.SELECTORS_PROPERTY;
import static com.freenow.sauron.plugins.KubernetesApiReport.SERVICE_LABEL_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesLabelAnnotationReaderTest
{
    @Spy
    private ApiClient client;

    private KubernetesApiReport plugin;


    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        plugin = new KubernetesApiReport(client);
    }


    @Test
    public void testKubernetesLabelAnnotationReaderEmptyAnnotationsLabelsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of(), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of("deployment", Map.of(
            "0", "test/label"
        )));
        plugin.apply(properties, dataSet);
        checkKeyNotPresent(dataSet, "test/label");
    }


    @Test
    public void testKubernetesLabelAnnotationReaderHaveAnnotationsEmptyLabelsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of("test/annotation", "myservice"), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of("deployment", Map.of(
            "0", "test/annotation"
        )));
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, "test/annotation", "myservice");
    }


    @Test
    public void testKubernetesLabelAnnotationReaderEmptyAnnotationHaveLabelsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of(), Map.of("test/label", "myservice")));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of("deployment", Map.of(
            "0", "test/label"
        )));
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, "test/label", "myservice");
    }


    @Test
    public void testKubernetesLabelAnnotationReaderWrongLabelsAnnotationsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of("test/annotation", "myservice"), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of("deployment", Map.of(
            "0", "test/wronglabel"
        )));
        plugin.apply(properties, dataSet);
        checkKeyNotPresent(dataSet, "test/wronglabel");
    }


    @Test
    public void testKubernetesLabelAnnotationReaderBothLabelsAnnotationsApply()
    {
        mockV1DeploymentListApiResponse(createDeploymentList(Map.of("test/annotation", "myserviceannotation"), Map.of("test/annotation", "myservicelabel")));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of("deployment", Map.of(
            "0", "test/annotation"
        )));
        plugin.apply(properties, dataSet);
        checkKeyPresent(dataSet, "test/annotation", "myserviceannotation");
    }


    @Test
    public void testKubernetesLabelAnnotationReaderCronjobApply()
    {
        mockV1BatchApiResponse(createJobList(Map.of("test/annotation", "myserviceannotation"), Map.of()));

        DataSet dataSet = createDataSet();
        PluginsConfigurationProperties properties = createPluginConfigurationProperties(Map.of("cronjob", Map.of("0", "test/annotation")));
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


    private PluginsConfigurationProperties createPluginConfigurationProperties(Map<String, Map<String, String>> selectors)
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put(PLUGIN_ID, Map.of(
            SERVICE_LABEL_PROPERTY, "serviceLabel",
            SELECTORS_PROPERTY, selectors)
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


    @SneakyThrows
    private void mockV1BatchApiResponse(V1beta1CronJobList list)
    {
        ApiResponse<V1beta1CronJobList> api = new ApiResponse<>(200, null, list);
        doReturn(api).when(client).execute(any(), eq(new TypeToken<V1beta1CronJobList>()
        {
        }.getType()));
    }


    private V1DeploymentList createDeploymentList(Map<String, String> annotations, Map<String, String> labels)
    {
        return new V1DeploymentList().items(List.of(new V1Deployment().metadata(createObjectMeta(annotations, labels))));
    }


    private V1beta1CronJobList createJobList(Map<String, String> annotations, Map<String, String> labels)
    {
        return new V1beta1CronJobList().items(List.of(new V1beta1CronJob().metadata(createObjectMeta(annotations, labels))));
    }


    private V1ObjectMeta createObjectMeta(Map<String, String> annotations, Map<String, String> labels)
    {
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setAnnotations(annotations);
        objectMeta.setLabels(labels);
        return objectMeta;
    }
}
