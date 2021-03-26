package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
public class LogsReportTest
{
    private static final Object LOGSTASH_PAYLOAD = "{\"query\":{\"bool\":{\"must\":[{\"query_string\":{\"analyze_wildcard\":true,\"query\":\"@tags: pla{ENVIRONMENT} AND @source: " +
        "{SERVICENAME}\"}}]," +
        "\"filter\":[{\"range\":{\"@timestamp\":{\"format\":\"basic_date_time\",\"gte\":\"now-10m\",\"lt\":\"now\"}}}]}}}";

    private static final String FILEBEAT_PAYLOAD = "{\"query\":{\"bool\":{\"filter\":[{\"match_phrase\":{\"mytaxi.environment\":{\"query\":\" {ENVIRONMENT}\"}}}," +
        "{\"match_phrase\":{\"mytaxi.service.name\":{\"query\":\"{SERVICENAME}\"}}},{\"range\":{\"@timestamp\":{\"format\":\"basic_date_time\",\"gte\":\"now-10m\",\"lt\":\"now\"}}}]}}}";

    public static final String NO_LOGS_FOUND = "{ \"took\": 146, \"hits\": { \"total\": { \"value\": 0 } } }";

    private final RestTemplate restTemplate = mock(RestTemplate.class);

    private final LogsReport plugin = new LogsReport(restTemplate);
    private final PluginsConfigurationProperties pluginsConfigurationProperties = dummyPluginConfigProperties();
    private final DataSet dataSet = dummyDataSet();


    @Test
    public void hasLogs()
    {
        when(restTemplate.exchange(contains("logstash-*"), eq(POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(
                ResponseEntity.of(Optional.of("{\n" +
                    "    \"took\": 146,\n" +
                    "    \"hits\": {\n" +
                    "        \"total\": {\n" +
                    "            \"value\": 9129\n" +
                    "        }\n" +
                    "    }\n" +
                    "}"))
            );

        plugin.apply(pluginsConfigurationProperties, dataSet);

        assertTrue(dataSet.getObjectAdditionalInformation("hasLogs").isPresent());
        assertThat((Boolean) dataSet.getObjectAdditionalInformation("hasLogs").get(), is(true));
        verify(restTemplate, times(1)).exchange(contains("logstash-*"), eq(POST), any(HttpEntity.class), eq(String.class));
    }


    @Test
    public void logsNotFound()
    {
        when(restTemplate.exchange(contains("logstash-*"), eq(POST), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.of(Optional.of(NO_LOGS_FOUND)));
        when(restTemplate.exchange(contains("filebeat-*"), eq(POST), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.of(Optional.of(NO_LOGS_FOUND)));

        plugin.apply(pluginsConfigurationProperties, dataSet);

        assertTrue(dataSet.getObjectAdditionalInformation("hasLogs").isPresent());
        assertFalse(dataSet.getBooleanAdditionalInformation("hasLogs").get());

        verify(restTemplate, times(1)).exchange(contains("logstash-*"), eq(POST), any(HttpEntity.class), eq(String.class));
        verify(restTemplate, times(1)).exchange(contains("filebeat-*"), eq(POST), any(HttpEntity.class), eq(String.class));
    }


    @Test
    public void logsNotFoundOnExceptions()
    {
        when(restTemplate.exchange(contains("logstash-*"), eq(POST), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.of(Optional.of(NO_LOGS_FOUND)));
        when(restTemplate.exchange(contains("filebeat-*"), eq(POST), any(HttpEntity.class), eq(String.class))).thenReturn(ResponseEntity.of(Optional.of(NO_LOGS_FOUND)));

        plugin.apply(pluginsConfigurationProperties, null);

        assertFalse(dataSet.getObjectAdditionalInformation("hasLogs").isPresent());
        verifyNoInteractions(restTemplate);
    }


    private PluginsConfigurationProperties dummyPluginConfigProperties()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = new PluginsConfigurationProperties();
        Map<String, Object> pluginProperties = new LinkedHashMap<>();

        pluginProperties.put("url", "https://elasticsearch-logs.mgmt.free-now.com/%s/_search?filter_path=took,hits.total.value");
        pluginProperties.put("user", "my-user");
        pluginProperties.put("password", "my-user");

        Map<String, Object> logStash = new LinkedHashMap<>();
        logStash.put("name", "logstash-*");
        logStash.put("payload", LOGSTASH_PAYLOAD);

        Map<String, Object> fileBeat = new LinkedHashMap<>();
        fileBeat.put("name", "filebeat-*");
        fileBeat.put("payload", FILEBEAT_PAYLOAD);

        Map<String, Object> zero = new LinkedHashMap<>();
        zero.put("0", logStash);
        zero.put("1", fileBeat);

        pluginProperties.put("indexes", zero);

        pluginsConfigurationProperties.put("logs-report", pluginProperties);
        return pluginsConfigurationProperties;
    }


    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName("my-service");
        dataSet.setAdditionalInformation("environment", "my-env");
        return dataSet;
    }

}
