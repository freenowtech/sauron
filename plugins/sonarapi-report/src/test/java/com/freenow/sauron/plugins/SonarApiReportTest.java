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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

public class SonarApiReportTest
{

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

    private final SonarApiReport plugin = new SonarApiReport(restTemplate);


    @Test
    public void testSonarApiReportApply()
    {
        Mockito.when(restTemplate.exchange(contains("com.mytaxi"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenThrow(HttpClientErrorException.class);

        Mockito.when(restTemplate.exchange(contains("com.freenow"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.of(Optional.of("{" +
            "    \"component\": {" +
            "        \"id\": \"AWikREXpXMApLJgibWTR\"," +
            "        \"key\": \"com.mytaxi:oauthservice:master\"," +
            "        \"name\": \"oauthservice master\"," +
            "        \"description\": \"OAuth service\"," +
            "        \"qualifier\": \"TRK\"," +
            "        \"measures\": [" +
            "            {" +
            "                \"metric\": \"coverage\"," +
            "                \"value\": \"73.5\"," +
            "                \"bestValue\": false" +
            "            }" +
            "        ]" +
            "    }" +
            "}")));

        final DataSet dataSet = new DataSet();
        dataSet.setServiceName("oauthservice");
        final Map<String, Object> properties = new HashMap();
        properties.put("baseUrl", "https://sonar.intapps.it");
        properties.put("accessToken", "dummyToken");
        Map fieldMap = new HashMap();
        fieldMap.put("uri", "/api/measures/component?component=?&metricKeys=coverage");
        fieldMap.put("fields", Collections.singletonMap("coverage", "$.component.measures[?(@.metric=='coverage')].value"));
        properties.put("api", Collections.singletonMap("measure-component", fieldMap));

        plugin.apply(createPluginsConfigurationProperties(properties), dataSet);

        checkKeyPresent(dataSet, "coverage", "73.5");
    }


    private PluginsConfigurationProperties createPluginsConfigurationProperties(final Map properties)
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = new PluginsConfigurationProperties();
        pluginsConfigurationProperties.put("sonarapi-report", properties);
        return pluginsConfigurationProperties;
    }


    private void checkKeyPresent(final DataSet dataSet, final String key, final Object expected)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }

}
