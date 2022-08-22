package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.plugins.utils.KubernetesResources;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV1Api;
import io.kubernetes.client.openapi.apis.BatchV1beta1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.NetworkingV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_API_TIMEOUT_SECONDS;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_DEFAULT_NAMESPACE;
import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_PRETTY_OUTPUT;

@Slf4j
public class KubernetesGetObjectMetaCommand
{
    private static final Random RANDOM = new Random();


    public Optional<V1ObjectMeta> get(String serviceLabel, KubernetesResources resource, String service, ApiClient client)
    {
        try
        {
            String labelSelector = String.format("%s=%s", serviceLabel, service);
            log.debug("Filtering resource {} using selector {}", resource, labelSelector);

            switch (resource)
            {
                case POD:
                    return new CoreV1Api(client).listNamespacedPod(
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
                    ).getItems().stream().sorted(KubernetesGetObjectMetaCommand::random).findAny().map(V1Pod::getMetadata);
                case SERVICE:
                    return new CoreV1Api(client).listNamespacedService(
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
                    ).getItems().stream().sorted(KubernetesGetObjectMetaCommand::random).findAny().map(V1Service::getMetadata);
                case DEPLOYMENT:
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
                    ).getItems().stream().sorted(KubernetesGetObjectMetaCommand::random).findAny().map(V1Deployment::getMetadata);
                case INGRESS:
                    return new NetworkingV1beta1Api(client).listNamespacedIngress(
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
                    ).getItems().stream().sorted(KubernetesGetObjectMetaCommand::random).findAny().map(NetworkingV1beta1Ingress::getMetadata);
                case CRONJOB:
                case JOB:
                    return new BatchV1beta1Api(client).listNamespacedCronJob(
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
                    ).getItems().stream().sorted(KubernetesGetObjectMetaCommand::random).findAny().map(V1beta1CronJob::getMetadata);
                case HORIZONTALPODAUTOSCALER:
                    return new AutoscalingV1Api(client).listNamespacedHorizontalPodAutoscaler(
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
                    ).getItems().stream().sorted(KubernetesGetObjectMetaCommand::random).findAny().map(V1HorizontalPodAutoscaler::getMetadata);
                default:
                    log.error("Invalid selector: {}", resource);
            }
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
