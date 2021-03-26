package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class JaegerApiReportTest
{
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

    private final JaegerApiReport plugin = new JaegerApiReport(restTemplate);


    @Test
    public void testJaegerApiReportApply()
    {
        Mockito.when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.of(Optional.of("{" +
            "  \"data\": [" +
            "    \"operation1\"," +
            "    \"operation2\"," +
            "    \"operation3\"" +
            "  ]," +
            "  \"total\": 3,\n" +
            "  \"limit\": 0,\n" +
            "  \"offset\": 0,\n" +
            "  \"errors\": null\n" +
            "}")));

        final DataSet dataSet = new DataSet();
        dataSet.setServiceName("xservice");
        final Map<String, Object> properties = new HashMap();
        properties.put("baseUrl", "https://jaegerui.com");
        Map fieldMap = new HashMap();
        fieldMap.put("uri", "/api/services/{serviceName}/operations");
        fieldMap.put("fields", Collections.singletonMap("jaeger_enabled", "#jsonPath(#body, '$.total') > 0"));
        properties.put("api", Collections.singletonMap("services-operations", fieldMap));

        plugin.apply(createPluginsConfigurationProperties(properties), dataSet);

        checkKeyPresent(dataSet, "jaeger_enabled", true);
    }


    private PluginsConfigurationProperties createPluginsConfigurationProperties(final Map properties)
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = new PluginsConfigurationProperties();
        pluginsConfigurationProperties.put("jaegerapi-report", properties);
        return pluginsConfigurationProperties;
    }


    private void checkKeyPresent(final DataSet dataSet, final String key, final Object expected)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }
}
