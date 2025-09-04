package com.freenow.sauron.plugins;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.*;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mock;
import com.freenow.sauron.plugins.commands.KubernetesGetDeploymentSpecCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import java.time.OffsetDateTime;
import java.util.Arrays;


public class KubernetesGetDeploymentSpecCommandTest
{
    @Test
    public void testGetDeploymentSpecReturnsLatestDeployment() throws ApiException {
        // Arrange
        ApiClient apiClient = mock(ApiClient.class);
        AppsV1Api appsV1Api = mock(AppsV1Api.class);

        V1DeploymentSpec spec1 = new V1DeploymentSpec().replicas(1);
        V1DeploymentSpec spec2 = new V1DeploymentSpec().replicas(2);

        V1Deployment deployment1 = new V1Deployment()
            .metadata(new V1ObjectMeta().name("old").creationTimestamp(OffsetDateTime.parse("2021-01-01T00:00:00Z")))
            .spec(spec1);

        V1Deployment deployment2 = new V1Deployment()
            .metadata(new V1ObjectMeta().name("new").creationTimestamp(OffsetDateTime.parse("2022-01-01T00:00:00Z")))
            .spec(spec2);

        V1DeploymentList deploymentList = new V1DeploymentList().items(Arrays.asList(deployment1, deployment2));

        when(appsV1Api.listNamespacedDeployment(anyString(), anyString(), anyBoolean(), any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
            .thenReturn(deploymentList);

        // Override createAppsV1Api to return your mock
        KubernetesGetDeploymentSpecCommand command = new KubernetesGetDeploymentSpecCommand() {
            @Override
            protected AppsV1Api createAppsV1Api(ApiClient client) {
                return appsV1Api;
            }
        };

        Optional<V1DeploymentSpec> result = command.getDeploymentSpec("label", null, "service", apiClient);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get().getReplicas());
    }
}
