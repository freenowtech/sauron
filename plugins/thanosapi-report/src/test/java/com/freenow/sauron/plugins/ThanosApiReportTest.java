package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ThanosApiReportTest
{
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

    private final ThanosApiReport plugin = new ThanosApiReport(restTemplate);


    @Test
    public void testJaegerApiReportApply()
    {
        Mockito.when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn("{" +
            "   \"status\":\"success\"," +
            "   \"data\":{" +
            "      \"resultType\":\"vector\"," +
            "      \"result\":[" +
            "         {" +
            "            \"metric\":{" +
            "               " +
            "            }," +
            "            \"value\":[" +
            "               1608133382.703," +
            "               \"817.9999999999999\"" +
            "            ]" +
            "         }" +
            "      ]" +
            "   }" +
            "}");

        final DataSet dataSet = new DataSet();
        dataSet.setServiceName("oauthservice");
        dataSet.setAdditionalInformation("environment", "live");
        final Map<String, Object> properties = new HashMap();
        properties.put("baseUrl", "https://thanos.mgmt.mytaxi.com");
        Map fieldMap = new HashMap();
        fieldMap.put("query", "sum(skipper:service_backend_requests_by_statuscode:1m{environment='$environment',service_name=~'$serviceName'})*60");
        fieldMap.put("fields", Collections.singletonMap("rpm", "$.data.result[0].value[1]"));
        properties.put("api", Collections.singletonMap("request-per-minute", fieldMap));

        plugin.apply(createPluginsConfigurationProperties(properties), dataSet);

        checkKeyPresent(dataSet, "rpm", "817.9999999999999");
    }


    @Test
    public void testJaegerApiReportApply_pathNotFound()
    {
        Mockito.when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn("{" +
            "   \"status\":\"success\"," +
            "   \"data\":{" +
            "      \"resultType\":\"vector\"," +
            "      \"result\":[]" +
            "   }" +
            "}");

        final DataSet dataSet = new DataSet();
        dataSet.setServiceName("oauthservice");
        dataSet.setAdditionalInformation("environment", "live");
        final Map<String, Object> properties = new HashMap();
        properties.put("baseUrl", "https://thanos.mgmt.mytaxi.com");
        Map fieldMap = new HashMap();
        fieldMap.put("query", "sum(skipper:service_backend_requests_by_statuscode:1m{environment='$environment',service_name=~'$serviceName'})*60");
        fieldMap.put("fields", Collections.singletonMap("rpm", "$.data.result[0].value[1]"));
        properties.put("api", Collections.singletonMap("request-per-minute", fieldMap));

        plugin.apply(createPluginsConfigurationProperties(properties), dataSet);

        checkKeyNotPresent(dataSet, "rpm");
    }


    private PluginsConfigurationProperties createPluginsConfigurationProperties(final Map properties)
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = new PluginsConfigurationProperties();
        pluginsConfigurationProperties.put("thanosapi-report", properties);
        return pluginsConfigurationProperties;
    }


    private void checkKeyPresent(final DataSet dataSet, final String key, final Object expected)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }


    private void checkKeyNotPresent(final DataSet dataSet, final String key)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isEmpty());
    }
}
