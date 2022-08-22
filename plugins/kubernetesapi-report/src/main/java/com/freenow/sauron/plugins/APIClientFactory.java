package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.KubernetesApiReport.API_CLIENT_CONFIG_PROPERTY;
import static com.freenow.sauron.plugins.KubernetesApiReport.PLUGIN_ID;
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


    public ApiClient get(final DataSet input, final PluginsConfigurationProperties properties)
    {
        if (apiClients.isEmpty())
        {
            properties.getPluginConfigurationProperty(PLUGIN_ID, API_CLIENT_CONFIG_PROPERTY)
                .ifPresent(config -> ((Map<String, String>) config).forEach((k, v) -> {
                    if (DEFAULT_CLIENT_CONFIG.equalsIgnoreCase(k))
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
                    else
                    {
                        apiClients.put(k, Config.fromUrl(v));
                    }
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
}
