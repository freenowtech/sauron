package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.plugins.utils.KubernetesResources;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import java.util.Comparator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_API_TIMEOUT_SECONDS;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_DEFAULT_NAMESPACE;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_PRETTY_OUTPUT;

@Slf4j
public class KubernetesGetDeploymentSpecCommand
{
    //defined to let us override it in the tests and inject a mock
    protected AppsV1Api createAppsV1Api(ApiClient client)
    {
        return new AppsV1Api(client);
    }


    public Optional<V1DeploymentSpec> getDeploymentSpec(String serviceLabel, KubernetesResources resource, String service, ApiClient client)
    {
        String labelSelector = String.format("%s=%s", serviceLabel, service);
        try
        {
            log.debug("Filtering resource {} using selector {}", resource, labelSelector);
            var deployments = createAppsV1Api(client).listNamespacedDeployment(
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
                )
                .getItems();

            if (deployments.isEmpty())
            {
                log.warn("No deployment found for service '{}' with selector '{}'", service, labelSelector);
                return Optional.empty();
            }

            return deployments.stream()
                .max(Comparator.comparing(
                    d -> {
                        if (d.getMetadata() == null)
                        {
                            return null;
                        }
                        return d.getMetadata().getCreationTimestamp();
                    }, Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(V1Deployment::getSpec);
        }
        catch (ApiException ex)
        {
            log.error("getDeploymentSpec failed '{}'", ex.getMessage(), ex);
        }

        log.warn("No deployment returned for service '{}' using selector {}", service, labelSelector);
        return Optional.empty();
    }
}
