package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.plugins.utils.KubernetesResources;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_API_TIMEOUT_SECONDS;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_DEFAULT_NAMESPACE;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_PRETTY_OUTPUT;

@Slf4j
public class KubernetesGetDeploymentSpecCommand
{
    public Optional<V1DeploymentSpec> getDeploymentSpec(String serviceLabel, KubernetesResources resource, String service, ApiClient client)
    {
        try
        {
            String labelSelector = String.format("%s=%s", serviceLabel, service);
            log.debug("Filtering deployment {} using selector {}", resource, labelSelector);
            return new AppsV1Api(client).listNamespacedDeployment(
                            K8S_DEFAULT_NAMESPACE,
                            K8S_PRETTY_OUTPUT,
                            false,
                            null,
                            null,
                            labelSelector,
                            null,
                            null,
                            null,
                            K8S_API_TIMEOUT_SECONDS,
                            false
                    ).getItems().stream().min(Comparator.comparing(
                            d -> {
                                if (d.getMetadata() == null) return null;
                                return d.getMetadata().getCreationTimestamp();
                            }, Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .map(V1Deployment::getSpec);
        }
        catch (ApiException ex)
        {
            log.error(ex.getMessage(), ex);
        }

        return Optional.empty();
    }
}
