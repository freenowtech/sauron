package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.codec.binary.Base64;
import org.pf4j.Extension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Extension
public class SonarApiReport implements SauronExtension
{

    public static final String PLUGIN_ID = "sonarapi-report";
    public static final String BASE_URL_CONFIG_PROPERTY = "baseUrl";
    public static final String ACCESS_TOKEN_CONFIG_PROPERTY = "accessToken";
    public static final String API_CONFIG_PROPERTY = "api";
    public static final String URI_CONFIG_PROPERTY = "uri";
    public static final String FIELDS_CONFIG_PROPERTY = "fields";
    private final RestTemplate restTemplate;

    private String baseUrl;

    private String accessToken;


    public SonarApiReport()
    {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setConnectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setReadTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        this.restTemplate = new RestTemplate(httpRequestFactory);
    }


    public SonarApiReport(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        properties.getPluginConfigurationProperty(PLUGIN_ID, BASE_URL_CONFIG_PROPERTY).ifPresent(url ->
        {
            baseUrl = String.valueOf(url);
            accessToken = (String) properties.getPluginConfigurationProperty(PLUGIN_ID, ACCESS_TOKEN_CONFIG_PROPERTY).orElse(null);
            final String serviceName = input.getServiceName();
            try
            {
                properties.getPluginConfigurationProperty(PLUGIN_ID, API_CONFIG_PROPERTY)
                    .ifPresent(api -> ((Map<String, Map<String, Object>>) api)
                        .forEach((apiName, valuesMap) -> setScanPropertiesInformation(input, serviceName, valuesMap)));
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        });
        return input;
    }


    private void setScanPropertiesInformation(DataSet input, String serviceName, Map<String, Object> valuesMap)
    {
        if (!valuesMap.containsKey(URI_CONFIG_PROPERTY) || !valuesMap.containsKey(FIELDS_CONFIG_PROPERTY))
        {
            return;
        }
        final String uri = (String) valuesMap.get(URI_CONFIG_PROPERTY);
        get(uri, serviceName.toLowerCase())
            .ifPresent(jsonBody -> ((Map<String, String>) valuesMap.get(FIELDS_CONFIG_PROPERTY))
                .forEach((key, expression) ->
                {
                    try
                    {
                        Object result = evaluate(jsonBody, expression);
                        input.setAdditionalInformation(key, result);
                    }
                    catch (Exception ex)
                    {
                        log.debug("Can't evaluate [key: expression]: [{}: {}].", key, expression, ex);
                    }
                }));
    }


    private Optional<String> get(final String uri, final String serviceName)
    {
        Optional<String> result = get(uri, serviceName, "com.mytaxi");
        if (result.isEmpty())
        {
            result = get(uri, serviceName, "com.freenow");
        }
        return result;
    }


    private Optional<String> get(final String uri, final String serviceName, final String groupId)
    {
        final String requestUrl = buildUrl(uri, serviceName, groupId);
        try
        {
            ResponseEntity<String> result = restTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(createHeaders()), String.class);
            return Optional.ofNullable(result.getBody());
        }
        catch (Exception ex)
        {
            log.debug("Request: [{}] failed.", requestUrl, ex);
        }
        return Optional.empty();
    }


    private String buildUrl(final String uri, final String serviceName, final String groupId)
    {
        return (baseUrl + uri).replace("component=?", "component=" + groupId + ":" + serviceName + ":master");
    }


    private HttpHeaders createHeaders()
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        final String auth = accessToken + ":";
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
        httpHeaders.set("Authorization", "Basic " + new String(encodedAuth));
        return httpHeaders;
    }


    private Object evaluate(final String jsonBody, final String expression)
    {
        final Object result = JsonPath.read(jsonBody, expression);
        return result instanceof JSONArray && !((JSONArray) result).isEmpty() ? ((JSONArray) result).get(0).toString() : result.toString();
    }
}
