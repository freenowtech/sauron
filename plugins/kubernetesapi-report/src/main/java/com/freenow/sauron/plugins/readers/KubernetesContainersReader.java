package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesGetDeploymentSpecCommand;
import com.freenow.sauron.plugins.utils.ContainerCheckStrategy;
import com.freenow.sauron.plugins.utils.LivenessCheckStrategy;
import com.freenow.sauron.plugins.utils.NoContainerCheckStrategy;
import com.freenow.sauron.plugins.utils.ReadinessCheckStrategy;
import com.freenow.sauron.plugins.utils.RetryCommand;
import com.freenow.sauron.plugins.utils.RetryConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    public static final String LIVENESS = "liveness";
    public static final String READINESS = "readiness";
    public static final String PROBE_PATH = "probePath";
    public static final String NOCHECK = "no-check";

    public static final Map<String, ContainerCheckStrategy> strategies = Map.of(
        LIVENESS, new LivenessCheckStrategy(),
        READINESS, new ReadinessCheckStrategy(),
        NOCHECK, new NoContainerCheckStrategy()
    );


    public KubernetesContainersReader()
    {
        this.kubernetesGetDeploymentSpecCommand = new KubernetesGetDeploymentSpecCommand();
        this.retryConfig = new RetryConfig();
    }


    public void read(DataSet input, String serviceLabel, Collection<String> containersCheck, ApiClient apiClient)
    {
        List<ContainerCheckStrategy> strategiesToApply = containersCheck.stream()
            .map(name -> strategies.getOrDefault(name, strategies.get(NOCHECK)))
            .collect(Collectors.toList());

        new RetryCommand<Void>(retryConfig).run(() ->
        {
            Optional<V1DeploymentSpec> deploymentSpecOpt = kubernetesGetDeploymentSpecCommand.getDeploymentSpec(
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
                        V1PodSpec podSpec = deploymentSpec.getTemplate().getSpec();
                        if (podSpec == null)
                        {
                            log.warn("deployment by name: {} doesn't have spec", deploymentName);
                            return;
                        }
                        for (V1Container container : podSpec.getContainers())
                        {
                            for (ContainerCheckStrategy strategy : strategiesToApply)
                            {
                                strategy.check(container, input);
                            }
                        }
                        checkAndSetProbePath(input, podSpec);
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


    private void checkAndSetProbePath(DataSet input, V1PodSpec podSpec)
    {
        if (podSpec == null || podSpec.getContainers() == null)
        {
            return;
        }

        boolean hasProbe = podSpec.getContainers().stream()
            .anyMatch(container -> container.getLivenessProbe() != null || container.getReadinessProbe() != null);

        if (hasProbe)
        {
            input.setAdditionalInformation(PROBE_PATH, "true");
        }
    }
}
