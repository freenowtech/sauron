package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesGetDeploymentSpecCommand;
import com.freenow.sauron.plugins.utils.ContainerCheckStrategy;
import com.freenow.sauron.plugins.utils.LivenessCheckStrategy;
import com.freenow.sauron.plugins.utils.ReadinessCheckStrategy;
import com.freenow.sauron.plugins.utils.RetryCommand;
import com.freenow.sauron.plugins.utils.RetryConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1PodSpec;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesResources.DEPLOYMENT;

@Slf4j
@RequiredArgsConstructor
public class KubernetesContainersReader
{
    private final KubernetesGetDeploymentSpecCommand kubernetesGetDeploymentSpecCommand;
    private final RetryConfig retryConfig;
    public static final String LIVENESS = "liveness";
    public static final String READINESS = "readiness";
    public static final String HAS_HEALTH_CHECK = "hasHealthCheck";

    public static final Map<String, ContainerCheckStrategy> strategies = Map.of(
        LIVENESS, new LivenessCheckStrategy(),
        READINESS, new ReadinessCheckStrategy()
    );


    public KubernetesContainersReader()
    {
        this.kubernetesGetDeploymentSpecCommand = new KubernetesGetDeploymentSpecCommand();
        this.retryConfig = new RetryConfig();
    }


    public void read(DataSet input, String serviceLabel, Collection<String> containersCheck, ApiClient apiClient)
    {
        List<ContainerCheckStrategy> strategiesToApply = containersCheck.stream()
            .peek(check -> {
                if (!strategies.containsKey(check))
                {
                    log.error("Missing container check implementation for: {} - proceeding with available checks...", check);
                }
            })
            .map(strategies::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        new RetryCommand<Void>(retryConfig).run(() ->
        {
            Optional<V1DeploymentSpec> deploymentSpecOpt = kubernetesGetDeploymentSpecCommand.getDeploymentSpec(
                serviceLabel,
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
                            log.warn("Deployment by name: {} doesn't have spec", deploymentName);
                            return;
                        }
                        for (V1Container container : podSpec.getContainers())
                        {
                            for (ContainerCheckStrategy strategy : strategiesToApply)
                            {
                                strategy.check(container, input);
                            }
                        }
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

            setHasHealthCheckPath(input, containersCheck);
            return null;
        });
    }


    /*
     * Sets the hasHealthCheck flag in the dataset if any of the specified health check probes are present, giving priority to the liveness probe.
     */
    private void setHasHealthCheckPath(DataSet input, final Collection<String> containersCheck)
    {
        boolean hasHealthCheck =
            input.getBooleanAdditionalInformation(LIVENESS).orElse(false) ||
                containersCheck.stream()
                    .anyMatch(check -> input.getBooleanAdditionalInformation(check).orElse(false));

        input.setAdditionalInformation(HAS_HEALTH_CHECK, hasHealthCheck);
    }
}
