package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.BatchV1beta1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.NetworkingV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
public class KubernetesApiReport implements SauronExtension
{
    private static final String DEFAULT_NAMESPACE = "default";

    private static final String FALSE = Boolean.FALSE.toString();

    private static final int TIMEOUT_SECONDS = 5;

    private static final String PLUGIN_ID = "kubernetesapi-report";

    private static final String SERVICE_LABEL_PROPERTY = "serviceLabel";

    private static final String SELECTORS_PROPERTY = "selectors";

    private final ApiClient client;


    public KubernetesApiReport() throws IOException
    {
        client = Config.defaultClient();
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        properties.getPluginConfigurationProperty(PLUGIN_ID, SERVICE_LABEL_PROPERTY).ifPresent(serviceLabel ->
            properties.getPluginConfigurationProperty(PLUGIN_ID, SELECTORS_PROPERTY).ifPresent(resourceFiltersProperty ->
            {
                try
                {
                    Map<?, ?> resourceFilters = ((Map<String, Map<?, ?>>) resourceFiltersProperty);
                    for (Map.Entry<?, ?> entry : resourceFilters.entrySet())
                    {
                        Optional<V1ObjectMeta> objectMetaOpt = getObjectMeta(String.valueOf(serviceLabel), String.valueOf(entry.getKey()), input.getServiceName());

                        if (objectMetaOpt.isPresent())
                        {
                            for (Object filter : castFilters(entry.getValue()))
                            {
                                trySetValue(input, String.valueOf(filter), objectMetaOpt.get().getAnnotations());
                                trySetValue(input, String.valueOf(filter), objectMetaOpt.get().getLabels());
                            }
                            break;
                        }
                    }
                }
                catch (Exception ex)
                {
                    log.error(ex.getMessage(), ex);
                }
            }));

        return input;
    }


    private Optional<V1ObjectMeta> getObjectMeta(String serviceLabel, String resource, String service)
    {
        try
        {
            String labelSelector = String.format("%s=%s", serviceLabel, service);

            log.debug("Filtering resource {} using selector {}", resource, labelSelector);

            switch (KubernetesResources.fromString(resource))
            {
                case POD:
                    return new CoreV1Api(client).listNamespacedPod(
                        DEFAULT_NAMESPACE,
                        FALSE,
                        false,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        null,
                        TIMEOUT_SECONDS,
                        false
                    ).getItems().stream().findFirst().map(V1Pod::getMetadata);
                case SERVICE:
                    return new CoreV1Api(client).listNamespacedService(
                        DEFAULT_NAMESPACE,
                        FALSE,
                        false,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        null,
                        TIMEOUT_SECONDS,
                        false
                    ).getItems().stream().findFirst().map(V1Service::getMetadata);
                case DEPLOYMENT:
                    return new AppsV1Api(client).listNamespacedDeployment(
                        DEFAULT_NAMESPACE,
                        FALSE,
                        false,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        null,
                        TIMEOUT_SECONDS,
                        false
                    ).getItems().stream().findFirst().map(V1Deployment::getMetadata);
                case INGRESS:
                    return new NetworkingV1beta1Api(client).listNamespacedIngress(
                        DEFAULT_NAMESPACE,
                        FALSE,
                        false,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        null,
                        TIMEOUT_SECONDS,
                        false
                    ).getItems().stream().findFirst().map(NetworkingV1beta1Ingress::getMetadata);
                case CRONJOB:
                case JOB:
                    return new BatchV1beta1Api(client).listNamespacedCronJob(
                        DEFAULT_NAMESPACE,
                        FALSE,
                        false,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        null,
                        TIMEOUT_SECONDS,
                        false
                    ).getItems().stream().findFirst().map(V1beta1CronJob::getMetadata);
                case HORIZONTALPODAUTOSCALER:
                    return new AutoscalingV1Api(client).listNamespacedHorizontalPodAutoscaler(
                        DEFAULT_NAMESPACE,
                        FALSE,
                        false,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        null,
                        TIMEOUT_SECONDS,
                        false
                    ).getItems().stream().findFirst().map(V1HorizontalPodAutoscaler::getMetadata);
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


    private void trySetValue(DataSet input, String key, Map<String, String> map)
    {
        if (map != null)
        {
            log.debug("Trying to get annotation/label {} from {}", key, map.keySet());
            if (input.getStringAdditionalInformation(key).isEmpty() && map.containsKey(key))
            {
                log.debug("Key {} found. Storing in Sauron Document", key);
                input.setAdditionalInformation(key, map.get(key));
            }
        }
    }


    private Collection<?> castFilters(Object filters)
    {
        return Optional.ofNullable(filters)
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(Map::values)
            .orElse(Collections.emptyList());
    }
}