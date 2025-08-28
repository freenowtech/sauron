package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesGetDeploymentSpecCommand;
import com.freenow.sauron.plugins.utils.RetryCommand;
import com.freenow.sauron.plugins.utils.RetryConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

import static com.freenow.sauron.plugins.utils.KubernetesResources.DEPLOYMENT;

@Slf4j
@RequiredArgsConstructor
public class KubernetesContainersReader
{
    private final KubernetesGetDeploymentSpecCommand kubernetesGetDeploymentSpecCommand;
    private final RetryConfig retryConfig;
    public static final String PRODUCTION_READINESS = "productionReadiness";

    public KubernetesContainersReader()
    {
        this.kubernetesGetDeploymentSpecCommand = new KubernetesGetDeploymentSpecCommand();
        this.retryConfig = new RetryConfig();
    }

    public void read(DataSet input, String serviceLabel, ApiClient apiClient)
    {
        new RetryCommand<Void>(retryConfig).run(() ->
        {
            Optional<V1DeploymentSpec> deploymentSpecOpt = kubernetesGetDeploymentSpecCommand.getDeployment(
                    String.valueOf(serviceLabel),
                    DEPLOYMENT,
                    input.getServiceName(),
                    apiClient
            );

            if (deploymentSpecOpt.isPresent())
            {
                final String deploymentName = Objects.requireNonNull(deploymentSpecOpt.get().getTemplate().getMetadata()).getName();
                try
                {
                    deploymentSpecOpt.ifPresent(deploymentSpec -> {
                        assert deploymentSpec.getTemplate().getSpec() != null;
                        deploymentSpec.getTemplate().getSpec().getContainers().forEach(container -> {
                            boolean hasReadiness = container.getReadinessProbe() != null;
                                    input.setAdditionalInformation(PRODUCTION_READINESS, String.valueOf(hasReadiness));
                            }
                        );
                    });
                }
                catch (Exception e)
                {
                    log.warn("Failed to fetch deployment by name: {}", deploymentName, e);
                }
            }
            else
            {
                log.warn("Deployment not found for service: {}", input.getServiceName());
            }
            return null;
        });
    }
}
