package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesGetDeploymentSpecCommand;
import com.freenow.sauron.plugins.readers.KubernetesContainersReader;
import com.freenow.sauron.plugins.utils.RetryConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Probe;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.utils.KubernetesResources.DEPLOYMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesContainersReaderTest
{
    private static final String SERVICE_LABEL = "label/service.name";
    private static final String SERVICE_NAME = "serviceName";

    @Mock
    private ApiClient apiClient;
    @Mock
    private KubernetesGetDeploymentSpecCommand kubernetesGetDeploymentSpecCommand;
    @InjectMocks
    private KubernetesContainersReader kubernetesContainersReader;


    @Before
    public void setUp()
    {
        kubernetesContainersReader = new KubernetesContainersReader(kubernetesGetDeploymentSpecCommand, new RetryConfig());
    }


    @Test
    public void deploymentIsReadyOnly()
    {
        final var deploymentData = createDeploymentData(true, false, false);
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient)).thenReturn(deploymentData);
        kubernetesContainersReader.read(input, SERVICE_LABEL, containersCheck(), apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).get());
        assertEquals("false", input.getStringAdditionalInformation(KubernetesContainersReader.LIVENESS).get());
    }


    @Test
    public void deploymentIsLiveOnly()
    {
        final var deploymentData = createDeploymentData(false, true, false);
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient)).thenReturn(deploymentData);
        kubernetesContainersReader.read(input, SERVICE_LABEL, containersCheck(), apiClient);
        assertNotNull(input);
        assertEquals("false", input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).get());
        assertEquals("true", input.getStringAdditionalInformation(KubernetesContainersReader.LIVENESS).get());
    }


    @Test
    public void deploymentIsLiveAndReady()
    {
        final var deploymentData = createDeploymentData(true, true, false);
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient)).thenReturn(deploymentData);

        kubernetesContainersReader.read(input, SERVICE_LABEL, containersCheck(), apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).get());
        assertEquals("true", input.getStringAdditionalInformation(KubernetesContainersReader.LIVENESS).get());
    }


    @Test
    public void deploymentIsNotReady()
    {
        final var deploymentData = createDeploymentData(false, false, false);
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient)).thenReturn(deploymentData);
        kubernetesContainersReader.read(input, SERVICE_LABEL, containersCheck(), apiClient);
        assertNotNull(input);
        assertEquals("false", input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).get());
        assertEquals("false", input.getStringAdditionalInformation(KubernetesContainersReader.LIVENESS).get());
    }


    @Test
    public void deploymentNotFoundSetsNoReadiness()
    {
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient))
            .thenReturn(Optional.empty());
        kubernetesContainersReader.read(input, SERVICE_LABEL, containersCheck(), apiClient);
        assertFalse(input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).isPresent());
    }


    @Test
    public void deploymentTemplateSpecIsNull()
    {
        final var input = dummyDataSet();
        final var deploymentData = createDeploymentData(true, false, true);
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient))
            .thenReturn(deploymentData);
        kubernetesContainersReader.read(input, SERVICE_LABEL, containersCheck(), apiClient);
        assertFalse(input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).isPresent());
    }


    @Test
    public void containersReadinessIsNotRequested()
    {
        final var input = dummyDataSet();
        final var deploymentData = createDeploymentData(true, false, false);
        when(kubernetesGetDeploymentSpecCommand.getDeploymentSpec(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient))
            .thenReturn(deploymentData);
        kubernetesContainersReader.read(input, SERVICE_LABEL, emptyList(), apiClient);
        assertFalse(input.getStringAdditionalInformation(KubernetesContainersReader.READINESS).isPresent());
    }


    private Optional<V1DeploymentSpec> createDeploymentData(boolean readinessProbe, boolean livenessProbe, boolean isNullSpec)
    {
        V1Container container = new V1Container().name("test-container")
            .livenessProbe(livenessProbe ? new V1Probe() : null)
            .readinessProbe(readinessProbe ? new V1Probe() : null);

        V1PodSpec podSpec = new V1PodSpec().containers(List.of(container));
        V1PodTemplateSpec template = new V1PodTemplateSpec()
            .spec(isNullSpec ? null : podSpec)
            .metadata(new V1ObjectMeta().name("test-deployment"));

        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
            .replicas(10)
            .template(template);

        return Optional.of(deploymentSpec);
    }


    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName(SERVICE_NAME);
        return dataSet;
    }


    private Collection<String> containersCheck()
    {
        return List.of(KubernetesContainersReader.READINESS, KubernetesContainersReader.LIVENESS);
    }


    private Collection<String> emptyList()
    {
        return List.of();
    }
}
