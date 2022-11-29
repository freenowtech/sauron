package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.readers.KubernetesEnvironmentVariablesReader;
import com.freenow.sauron.plugins.readers.KubernetesLabelAnnotationReader;
import com.freenow.sauron.plugins.readers.KubernetesPropertiesFilesReader;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
@NoArgsConstructor
public class KubernetesApiReport implements SauronExtension
{
    static final String PLUGIN_ID = "kubernetesapi-report";
    static final String SERVICE_LABEL_PROPERTY = "serviceLabel";
    static final String API_CLIENT_CONFIG_PROPERTY = "apiClientConfig";
    static final String SELECTORS_PROPERTY = "selectors";
    static final String ENV_VARS_PROPERTY = "environmentVariablesCheck";
    static final String PROPERTIES_FILES_CHECK = "propertiesFilesCheck";

    private APIClientFactory apiClientFactory = new APIClientFactory();
    private KubernetesLabelAnnotationReader kubernetesLabelAnnotationReader = new KubernetesLabelAnnotationReader();
    private KubernetesEnvironmentVariablesReader kubernetesEnvironmentVariablesReader = new KubernetesEnvironmentVariablesReader();
    private KubernetesPropertiesFilesReader kubernetesPropertiesFilesReader = new KubernetesPropertiesFilesReader();


    public KubernetesApiReport(
        final APIClientFactory apiClientFactory,
        final KubernetesLabelAnnotationReader kubernetesLabelAnnotationReader,
        final KubernetesEnvironmentVariablesReader kubernetesEnvironmentVariablesReader,
        final KubernetesPropertiesFilesReader kubernetesPropertiesFilesReader
    )
    {
        this.apiClientFactory = apiClientFactory;
        this.kubernetesLabelAnnotationReader = kubernetesLabelAnnotationReader;
        this.kubernetesEnvironmentVariablesReader = kubernetesEnvironmentVariablesReader;
        this.kubernetesPropertiesFilesReader = kubernetesPropertiesFilesReader;

    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        final var apiClient = apiClientFactory.get(input, properties);

        properties.getPluginConfigurationProperty(PLUGIN_ID, SERVICE_LABEL_PROPERTY).map(String.class::cast).ifPresent(serviceLabel ->
        {
            properties.getPluginConfigurationProperty(PLUGIN_ID, SELECTORS_PROPERTY).ifPresent(resourceFilters ->
                kubernetesLabelAnnotationReader.read(input, serviceLabel, (Map<String, Map<?, ?>>) resourceFilters, apiClient)
            );

            properties.getPluginConfigurationProperty(PLUGIN_ID, ENV_VARS_PROPERTY).filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(Map::values)
                .ifPresent(envVarsProperty -> kubernetesEnvironmentVariablesReader.read(input, serviceLabel, envVarsProperty, apiClient));

            properties.getPluginConfigurationProperty(PLUGIN_ID, PROPERTIES_FILES_CHECK).filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(Map.class::cast)
                .ifPresent(propFilesCheck -> kubernetesPropertiesFilesReader.read(input, serviceLabel, propFilesCheck, apiClient));
        });
        return input;
    }
}