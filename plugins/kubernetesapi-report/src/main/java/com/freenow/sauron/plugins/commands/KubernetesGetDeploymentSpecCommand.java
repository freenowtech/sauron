package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.plugins.utils.KubernetesResources;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Random;

import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_API_TIMEOUT_SECONDS;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_DEFAULT_NAMESPACE;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_PRETTY_OUTPUT;

@Slf4j
public class KubernetesGetDeploymentSpecCommand
{
    private static final Random RANDOM = new Random();

    public Optional<V1DeploymentSpec> getDeployment(String serviceLabel, KubernetesResources resource, String service, ApiClient client)
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
            ).getItems().stream().sorted(KubernetesGetDeploymentSpecCommand::random).findAny().map(V1Deployment::getSpec);
        }
        catch (ApiException ex)
        {
            log.error(ex.getMessage(), ex);
        }

        return Optional.empty();
    }

    private static int random(Object f1, Object f2)
    {
        return (RANDOM.nextInt(2)) == 0 ? -1 : 1;
    }
}
