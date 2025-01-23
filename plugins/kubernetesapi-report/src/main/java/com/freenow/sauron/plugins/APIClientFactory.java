package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.KubernetesApiReport.API_CLIENT_CONFIG_PROPERTY;
import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
import static io.kubernetes.client.util.KubeConfig.ENV_HOME;
import static io.kubernetes.client.util.KubeConfig.KUBECONFIG;
import static io.kubernetes.client.util.KubeConfig.KUBEDIR;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@NoArgsConstructor
public class APIClientFactory
{
    private static final String DEFAULT_CLIENT_CONFIG = "default";

    private static final String ENVIRONMENT = "environment";

    private Map<String, ApiClient> apiClients = new HashMap<>();


    public APIClientFactory(final Map<String, ApiClient> apiClients)
    {
        this.apiClients = apiClients;
    }


    /**
     * Creates the Kubernetes API client for an environment.
     * It reads the environment from the field "environment" in the DataSet.
     * If no client for the environment can be found, then it falls back to a default client.
     * <p>
     * The configuration of this plugin supports multiple ways to create an API client:
     * <pre>
     *     kubernetesapi-report:
     *       # ...
     *       apiClientConfig:
     *         default: default # Use the default client
     *         clusterOne: "https://clusterOne.local" # Use a URL
     *         clusterTwo: clusterTwo # Use the context "clusterTwo" from the kube config file at $HOME/.kube/config
     *       # ...
     * </pre>
     *
     * @param input The current DataSet.
     * @param properties Plugin configuration.
     * @return Kubernetes API client.
     */
    public ApiClient get(final DataSet input, final PluginsConfigurationProperties properties)
    {
        if (apiClients.isEmpty())
        {
            properties.getPluginConfigurationProperty(PLUGIN_ID, API_CLIENT_CONFIG_PROPERTY)
                .ifPresent(config -> ((Map<String, String>) config).forEach((k, v) -> {
                    apiClients.put(k, createClient(k, v));
                }));

            if (apiClients.isEmpty())
            {
                try
                {
                    apiClients.put(DEFAULT_CLIENT_CONFIG, Config.defaultClient());
                }
                catch (Exception e)
                {
                    log.error("API Client not initialized. Error: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        }
        return Optional.ofNullable(apiClients.get(input.getStringAdditionalInformation(ENVIRONMENT).orElse(EMPTY))).orElse(apiClients.get(DEFAULT_CLIENT_CONFIG));
    }

    private ApiClient createClient(String cluster, String value)
    {
        try
        {
            if (DEFAULT_CLIENT_CONFIG.equalsIgnoreCase(cluster))
            {
                log.debug("Creating default Kubernetes client for cluster {}", cluster);
                return Config.defaultClient();
            }

            if (value.startsWith("https://"))
            {
                log.debug("Creating Kubernetes client from URL {} for cluster {}", value, cluster);
                return Config.fromUrl(value);
            }

            log.debug("Creating Kubernetes client from config for cluster {}", cluster);
            // Create KubeConfig here because it allows setting the context.
            File configFile = new File(new File(System.getenv(ENV_HOME), KUBEDIR), KUBECONFIG);
            KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(configFile));
            kubeConfig.setContext(value);
            return ClientBuilder.kubeconfig(kubeConfig).build();
        }
        catch (Exception e)
        {
            log.error("API Client for {} not initialized. Error: {}", cluster, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
