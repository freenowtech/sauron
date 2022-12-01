package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.readers.KubernetesEnvironmentVariablesReader;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.utils.KubernetesResources.POD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesEnvironmentVariablesReaderTest
{
    private static final String SERVICE_LABEL = "label/service.name";
    private static final String SERVICE_NAME = "serviceName";
    private static final String ENV_COMMAND = "bash -l -c env | grep ^%s";
    private static final String ENV_ENABLED = "ENV_ENABLED";
    private static final String ENV_VERSION = "ENV_VERSION";

    @Mock
    private ApiClient apiClient;

    @Mock
    private KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand;

    @Mock
    private KubernetesExecCommand kubernetesExecCommand;

    @InjectMocks
    private KubernetesEnvironmentVariablesReader kubernetesEnvironmentVariablesReader;


    @Test
    public void read()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();

        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, "", apiClient)).thenReturn(objMetaData);
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, ENV_ENABLED), apiClient)).thenReturn(localEnvVars());
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, ENV_VERSION), apiClient)).thenReturn(localEnvVars());

        kubernetesEnvironmentVariablesReader.read(input, SERVICE_LABEL, envVars(), apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(ENV_ENABLED).get());
        assertEquals("8080", input.getStringAdditionalInformation(ENV_VERSION).get());
    }


    @Test
    public void noEnvVarsFoundOnPOD()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();

        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, "", apiClient)).thenReturn(objMetaData);
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, ENV_ENABLED), apiClient)).thenReturn(localEnvVars());
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, ENV_VERSION), apiClient)).thenReturn(Optional.empty());

        kubernetesEnvironmentVariablesReader.read(input, SERVICE_LABEL, envVars(), apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(ENV_ENABLED).get());
        assertFalse(input.getStringAdditionalInformation(ENV_VERSION).isPresent());
    }


    @Test
    public void readDontModifyInputWhenMetaDataNotFound()
    {
        final var input = dummyDataSet();

        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, "", apiClient)).thenReturn(Optional.empty());

        kubernetesEnvironmentVariablesReader.read(input, SERVICE_LABEL, envVars(), apiClient);
        assertNotNull(input);
        assertEquals(input, dummyDataSet());
    }


    @Test
    public void readEnvVarsNotFound()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();

        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, POD, "", apiClient)).thenReturn(objMetaData);
        when(kubernetesExecCommand.exec(objMetaData.get().getName(), String.format(ENV_COMMAND, ENV_ENABLED), apiClient)).thenReturn(Optional.empty());

        kubernetesEnvironmentVariablesReader.read(input, SERVICE_LABEL, envVars(), apiClient);
        assertNotNull(input);
        assertFalse(input.getStringAdditionalInformation(ENV_ENABLED).isPresent());
    }


    private Optional<V1ObjectMeta> createObjMetaData()
    {
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setLabels(Map.of(SERVICE_LABEL, SERVICE_NAME));
        objectMeta.setName(SERVICE_NAME);

        return Optional.of(objectMeta);
    }


    private Optional<String> localEnvVars()
    {
        return Optional.of("ENV_ENABLED=true\n" + "ENV_VERSION=8080\n");
    }


    private Collection<String> envVars()
    {
        return List.of(ENV_ENABLED, ENV_VERSION);
    }


    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName("");
        return dataSet;
    }
}
