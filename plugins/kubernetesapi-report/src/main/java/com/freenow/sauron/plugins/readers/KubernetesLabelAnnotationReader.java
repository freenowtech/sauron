package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.utils.KubernetesResources;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class KubernetesLabelAnnotationReader
{
    private KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand = new KubernetesGetObjectMetaCommand();


    public KubernetesLabelAnnotationReader(final KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand)
    {
        this.kubernetesGetObjectMetaCommand = kubernetesGetObjectMetaCommand;
    }


    public void read(DataSet input, String serviceLabel, Map<String, Map<?, ?>> resourceFiltersProperty, ApiClient apiClient)
    {
        try
        {
            Map<?, ?> resourceFilters = resourceFiltersProperty;
            for (Map.Entry<?, ?> entry : resourceFilters.entrySet())
            {
                Optional<V1ObjectMeta> objectMetaOpt = kubernetesGetObjectMetaCommand.get(
                    String.valueOf(serviceLabel),
                    KubernetesResources.fromString(String.valueOf(entry.getKey())),
                    input.getServiceName(),
                    apiClient);

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
