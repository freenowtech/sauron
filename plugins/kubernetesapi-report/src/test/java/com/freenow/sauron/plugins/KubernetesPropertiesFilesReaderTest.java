package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.readers.KubernetesPropertiesFilesReader;
import com.freenow.sauron.plugins.utils.RetryConfig;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static com.freenow.sauron.plugins.utils.KubernetesResources.POD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesPropertiesFilesReaderTest
{
    private static final String SERVICE_LABEL = "label/service.name";
    private static final String SERVICE_NAME = "serviceName";
    private static final String ENV_COMMAND = "cat %s";
    private static final String PROP_FILE_ENV_ENABLED = "dummy.properties";
    private static final String PROP_FILE_ENV_VERSION = "dummy.env";
    private static final String ENV_ENABLED = "ENV_ENABLED";
    private static final String ENV_VERSION = "ENV_VERSION";
    private static final String ENV_ANOTHER = "ENV_ANOTHER";
    private static final String PROPERTIES_FILES_CHECK = "propertiesFilesCheck";

    @Mock
    private ApiClient apiClient;
    @Mock
    private KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand;
    @Mock
    private KubernetesExecCommand kubernetesExecCommand;
    @Mock
    private RetryConfig retryConfig;
    @InjectMocks
    private KubernetesPropertiesFilesReader kubernetesPropertiesFilesReader;


    @Test
    public void read()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();
        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, SERVICE_NAME, apiClient)).thenReturn(objMetaData);
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, PROP_FILE_ENV_ENABLED), apiClient)).thenReturn(envProps());
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, PROP_FILE_ENV_VERSION), apiClient)).thenReturn(envProps());

        kubernetesPropertiesFilesReader.read(input, SERVICE_LABEL, getPropertyFilesConfig(), apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(ENV_ENABLED).get());
        assertEquals("8080", input.getStringAdditionalInformation(ENV_VERSION).get());
        assertEquals("not_found",input.getStringAdditionalInformation(ENV_ANOTHER).get());
    }

    @Test
    public void twoPropertiesFilesRequestedToCheckButOneIsNotFound()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();
        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, SERVICE_NAME, apiClient)).thenReturn(objMetaData);
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, PROP_FILE_ENV_ENABLED), apiClient)).thenReturn(envProps());
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, PROP_FILE_ENV_VERSION), apiClient)).thenReturn(Optional.empty());

        kubernetesPropertiesFilesReader.read(input, SERVICE_LABEL, getPropertyFilesConfig(), apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(ENV_ENABLED).get());
        assertEquals("not_found", input.getStringAdditionalInformation(ENV_VERSION).get());
        assertEquals("not_found", input.getStringAdditionalInformation(ENV_ANOTHER).get());
    }


    @Test
    public void noPropertyFileFoundOnPOD()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();
        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, SERVICE_NAME, apiClient)).thenReturn(objMetaData);

        kubernetesPropertiesFilesReader.read(input, SERVICE_LABEL, getPropertyFilesConfig(), apiClient);
        assertNotNull(input);
        assertEquals("not_found", input.getStringAdditionalInformation(ENV_ENABLED).get());
        assertEquals("not_found", input.getStringAdditionalInformation(ENV_VERSION).get());
        assertEquals("not_found", input.getStringAdditionalInformation(ENV_ANOTHER).get());
    }


    @Test
    public void readDontModifyInputWhenMetaDataNotFound()
    {
        final var input = dummyDataSet();
        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, SERVICE_NAME, apiClient)).thenReturn(Optional.empty());

        kubernetesPropertiesFilesReader.read(input, SERVICE_LABEL, getPropertyFilesConfig(), apiClient);
        assertNotNull(input);
        assertEquals(input, dummyDataSet());
    }


    private Optional<String> envProps()
    {
        return Optional.of("env.is.enabled=true\n" + "ENV_VERSION=8080\n");
    }


    private PluginsConfigurationProperties dummyPluginConfig()
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        Map<String, Object> props = new HashMap<>();
        props.put("serviceLabel", SERVICE_LABEL);
        props.put(
            "propertiesFilesCheck",
            Map.of(
                "dummy.properties",
                Map.of(
                    "ENV_ENABLED", "env.is.enabled",
                    "ENV_ANOTHER", "env.is.not.found"
                ),
                "dummy.env", Map.of(
                    "ENV_VERSION", "ENV_VERSION"
                )
            )
        );

        properties.put(PLUGIN_ID, props);
        return properties;
    }


    private Map<String, Map<String, String>> getPropertyFilesConfig()
    {
        Optional<Map> pluginConfigs = dummyPluginConfig().getPluginConfigurationProperty(PLUGIN_ID, PROPERTIES_FILES_CHECK)
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(Map.class::cast);
        return pluginConfigs.get();
    }


    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName(SERVICE_NAME);
        return dataSet;
    }


    private Optional<V1ObjectMeta> createObjMetaData()
    {
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setLabels(Map.of(SERVICE_LABEL, SERVICE_NAME));
        objectMeta.setName(SERVICE_NAME);

        return Optional.of(objectMeta);
    }
}
