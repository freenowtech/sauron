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
public class KubernetesContainersReaderTest {
    private static final String SERVICE_LABEL = "label/service.name";
    private static final String SERVICE_NAME = "serviceName";
    public static final String PRODUCTION_READINESS = "productionReadiness";

    @Mock
    private ApiClient apiClient;
    @Mock
    private KubernetesGetDeploymentSpecCommand kubernetesGetDeploymentSpecCommand;
    @InjectMocks
    private KubernetesContainersReader kubernetesContainersReader;

    @Before
    public void setUp() {
        kubernetesContainersReader = new KubernetesContainersReader(kubernetesGetDeploymentSpecCommand, new RetryConfig());
    }

    @Test
    public void read()
    {
        final var deploymentData = createDeploymentData(true);
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeployment(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient)).thenReturn(deploymentData);

        kubernetesContainersReader.read(input, SERVICE_LABEL, apiClient);
        assertNotNull(input);
        assertEquals("true", input.getStringAdditionalInformation(PRODUCTION_READINESS).get());
    }

    @Test
    public void deploymentIsNotReady(){
        final var deploymentData = createDeploymentData(false);
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeployment(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient)).thenReturn(deploymentData);

        kubernetesContainersReader.read(input, SERVICE_LABEL, apiClient);
        assertNotNull(input);
        assertEquals("false", input.getStringAdditionalInformation(PRODUCTION_READINESS).get());
    }


    @Test
    public void deploymentNotFound_setsNoReadiness() {
        final var input = dummyDataSet();
        when(kubernetesGetDeploymentSpecCommand.getDeployment(SERVICE_LABEL, DEPLOYMENT, SERVICE_NAME, apiClient))
                .thenReturn(Optional.empty());

        kubernetesContainersReader.read(input, SERVICE_LABEL, apiClient);

        assertFalse(input.getStringAdditionalInformation(PRODUCTION_READINESS).isPresent());
    }

    private Optional<V1DeploymentSpec> createDeploymentData(boolean isReady) {
        V1Container container = new V1Container().name("test-container");
        if (isReady) {
            container.readinessProbe(new V1Probe());
        } else {
            container.readinessProbe(null);
        }
        V1PodSpec podSpec = new V1PodSpec().containers(List.of(container));
        V1PodTemplateSpec template = new V1PodTemplateSpec().spec(podSpec).metadata(new V1ObjectMeta().name("test-deployment"));
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec().replicas(10).template(template);
        return Optional.of(deploymentSpec);
    }

    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName(SERVICE_NAME);
        return dataSet;
    }
}
