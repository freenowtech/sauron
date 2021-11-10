package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.readers.KubernetesEnvironmentVariablesReader;
import com.freenow.sauron.plugins.readers.KubernetesLabelAnnotationReader;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
@RequiredArgsConstructor
public class KubernetesApiReport implements SauronExtension
{
    static final String PLUGIN_ID = "kubernetesapi-report";

    static final String SERVICE_LABEL_PROPERTY = "serviceLabel";

    static final String SELECTORS_PROPERTY = "selectors";

    static final String ENV_VARS_PROPERTY = "environmentVariablesCheck";

    private final KubernetesLabelAnnotationReader kubernetesLabelAnnotationReader;

    private final KubernetesEnvironmentVariablesReader kubernetesEnvironmentVariablesReader;


    public KubernetesApiReport() throws IOException
    {
        ApiClient client = Config.defaultClient();
        this.kubernetesLabelAnnotationReader = new KubernetesLabelAnnotationReader(client);
        this.kubernetesEnvironmentVariablesReader = new KubernetesEnvironmentVariablesReader(client);
    }


    public KubernetesApiReport(ApiClient client)
    {
        this.kubernetesLabelAnnotationReader = new KubernetesLabelAnnotationReader(client);
        this.kubernetesEnvironmentVariablesReader = new KubernetesEnvironmentVariablesReader(client);
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        properties.getPluginConfigurationProperty(PLUGIN_ID, SERVICE_LABEL_PROPERTY).map(String.class::cast).ifPresent(serviceLabel ->
        {
            properties.getPluginConfigurationProperty(PLUGIN_ID, SELECTORS_PROPERTY).ifPresent(resourceFilters ->
                kubernetesLabelAnnotationReader.read(input, serviceLabel, (Map<String, Map<?, ?>>) resourceFilters)
            );

            properties.getPluginConfigurationProperty(PLUGIN_ID, ENV_VARS_PROPERTY).filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(Map::values)
                .ifPresent(envVarsProperty ->
                kubernetesEnvironmentVariablesReader.read(input, serviceLabel, envVarsProperty)
            );
        });

        return input;
    }
}