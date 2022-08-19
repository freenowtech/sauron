package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.readers.KubernetesEnvironmentVariablesReader;
import com.freenow.sauron.plugins.readers.KubernetesLabelAnnotationReader;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.KubernetesApiReport.ENV_VARS_PROPERTY;
import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static com.freenow.sauron.plugins.KubernetesApiReport.SELECTORS_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesApiReportTest
{
    private static final String SERVICE_LABEL = "label/service.name";
    private static final String TEST_ANNOTATION = "test/annotation";
    private static final String TEST_LABEL = "test/label";
    @Mock
    private KubernetesLabelAnnotationReader kubernetesLabelAnnotationReader;

    @Mock
    private KubernetesEnvironmentVariablesReader kubernetesEnvironmentVariablesReader;

    @Mock
    private APIClientFactory apiClientFactory;

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private KubernetesApiReport kubernetesApiReport;


    @Test
    public void unModifiedDataSetWhenNoPluginProps()
    {
        final var properties = new PluginsConfigurationProperties();
        final var input = dummyDataSet();

        when(apiClientFactory.get(input, properties)).thenReturn(apiClient);

        final var result = kubernetesApiReport.apply(properties, input);
        assertEquals(dummyDataSet(), result);
        verifyZeroInteractions(kubernetesLabelAnnotationReader);
        verifyZeroInteractions(kubernetesEnvironmentVariablesReader);
    }


    @Test
    public void selectorsPropertyApplied()
    {
        final var input = dummyDataSet();
        final var pluginConfig = pluginConfig();
        pluginConfig.get(PLUGIN_ID).remove(ENV_VARS_PROPERTY);
        when(apiClientFactory.get(input, pluginConfig)).thenReturn(apiClient);

        Map<String, Map<?, ?>> selectors = dummySelectors();

        kubernetesApiReport.apply(pluginConfig, input);
        verify(kubernetesLabelAnnotationReader).read(input, SERVICE_LABEL, selectors, apiClient);
        verifyZeroInteractions(kubernetesEnvironmentVariablesReader);
    }


    @Test
    public void environmentVariablesChecked()
    {
        final var input = dummyDataSet();
        final var pluginConfig = pluginConfig();
        pluginConfig.get(PLUGIN_ID).remove(SELECTORS_PROPERTY);
        when(apiClientFactory.get(input, pluginConfig)).thenReturn(apiClient);

        final var vars = pluginConfig.getPluginConfigurationProperty(PLUGIN_ID, ENV_VARS_PROPERTY)
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(Map::values)
            .get();

        kubernetesApiReport.apply(pluginConfig, input);
        verify(kubernetesEnvironmentVariablesReader).read(input, SERVICE_LABEL, vars, apiClient);
        verifyZeroInteractions(kubernetesLabelAnnotationReader);
    }


    @Test
    public void allPropsChecked()
    {
        final var input = dummyDataSet();
        final var pluginConfig = pluginConfig();
        when(apiClientFactory.get(input, pluginConfig)).thenReturn(apiClient);
        Map<String, Map<?, ?>> selectors = dummySelectors();
        final var vars = pluginConfig.getPluginConfigurationProperty(PLUGIN_ID, ENV_VARS_PROPERTY)
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(Map::values)
            .get();

        kubernetesApiReport.apply(pluginConfig, input);
        verify(kubernetesLabelAnnotationReader).read(input, SERVICE_LABEL, selectors, apiClient);
        verify(kubernetesEnvironmentVariablesReader).read(input, SERVICE_LABEL, vars, apiClient);
        verifyNoMoreInteractions(kubernetesLabelAnnotationReader, kubernetesEnvironmentVariablesReader);
    }


    private static Map<String, Map<?, ?>> dummySelectors()
    {
        Map<String, Map<?, ?>> selectors = new HashMap<>();
        selectors.put("deployment", Map.of("0", TEST_ANNOTATION, "1", TEST_LABEL));
        selectors.put("cronjob", Map.of("0", TEST_ANNOTATION, "1", TEST_LABEL));
        return selectors;
    }


    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName("");
        return dataSet;
    }


    private PluginsConfigurationProperties pluginConfig()
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        Map<String, Object> props = new HashMap<>();
        props.put("serviceLabel", SERVICE_LABEL);
        props.put("selectors", dummySelectors());
        props.put("environmentVariablesCheck", Map.of("0", "ENV_ENABLED", "1", "ENV_VERSION"));
        props.put("apiClientConfig", Map.of(
            "default", "",
            "cluster-a", "https://kubernetes.cluster-a.com",
            "cluster-b", "https://kubernetes.cluster-b.com"
        ));

        properties.put(PLUGIN_ID, props);
        return properties;
    }
}
