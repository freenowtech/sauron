package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Webhook sends a request to an HTTP endpoint to trigger further processing.
 * Multiple endpoints can be configured.
 * <p>
 * Each configuration of an endpoint can have the following properties:
 * <p>
 * url - the full URL to send the request to. For example, http://example.localhost/trigger.
 * method - the HTTP method to use. Default to POST.
 * includeDataSet - boolean flag to indicate if the Sauron Data Set should be sent as part of the request.
 * Default is false, which means that no Data Set is sent.
 */
@Slf4j
@Extension
public class Webhook implements SauronExtension
{
    private static final String PLUGIN_ID = "webhook";
    private static final String PROPERTY_ENDPOINTS = "endpoints";
    private static final String PROPERTY_INCLUDE_DATASET = "includeDataSet";
    private static final String PROPERTY_METHOD = "method";
    private static final String PROPERTY_URL = "url";

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(Webhook.class.getName());

    public Webhook()
    {
        this.restTemplate = new RestTemplate();
    }

    public Webhook(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    /**
     * Implements SauronExtension.
     *
     * @param properties Configuration of the plugin.
     * @param input Data set currently being processed by Sauron.
     * @return DataSet
     */
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input) {
        properties.getPluginConfigurationProperty(PLUGIN_ID, PROPERTY_ENDPOINTS).ifPresent(endpointProperty -> {
            Map<String, Object> endpoints = (Map<String, Object>) endpointProperty;
            endpoints.forEach((name, data) -> {
                Map<String, Object> config = (Map<String, Object>) data;
                String url = (String) config.getOrDefault(PROPERTY_URL, "");
                String methodRaw = (String) config.getOrDefault(PROPERTY_METHOD, "POST");
                Boolean includeDataSet = (Boolean) config.getOrDefault(PROPERTY_INCLUDE_DATASET, false);
                send(name, url, methodRaw, includeDataSet, input);
            });
        });

        return input;
    }

    private void send(String name, String url, String methodRaw, Boolean includeDataSet, DataSet dataSet)
    {
        HttpMethod method = HttpMethod.resolve(methodRaw);
        if (method == null)
        {
            logger.error("HTTP method {} of webhook endpoint {} is invalid", name, methodRaw);
            return;
        }

        HttpEntity<DataSet> payload = null;
        if (includeDataSet)
        {
            payload = new HttpEntity<>(dataSet);
        }

        ResponseEntity<Object> response = restTemplate.exchange(url, method, payload, Object.class);
        if (!response.getStatusCode().is2xxSuccessful())
        {
            logger.error("Sending webhook {} to {} failed with HTTP status code {}", name, url, response.getStatusCode());
        }
    }
}
