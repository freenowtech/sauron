package com.freenow.sauron.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.replaceEach;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Extension
public class LogsReport implements SauronExtension
{
    private static final String LOGS_REPORT = "logs-report";
    private static final String URL = "url";
    private static final String USER = "user";
    private static final String PASS = "password";
    private static final String ENVIRONMENT = "environment";

    private static final int FIVE_SECONDS = 5000;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;


    public LogsReport()
    {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(FIVE_SECONDS);
        httpRequestFactory.setConnectTimeout(FIVE_SECONDS);
        httpRequestFactory.setReadTimeout(FIVE_SECONDS);
        this.restTemplate = new RestTemplate(httpRequestFactory);
        this.mapper = new ObjectMapper();
    }


    public LogsReport(final RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper();
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        properties.getPluginConfigurationProperty(LOGS_REPORT, URL).ifPresent(url ->
        {
            try
            {
                final String user = valueOf(properties.getPluginConfigurationProperty(LOGS_REPORT, USER).get());
                final String pass = valueOf(properties.getPluginConfigurationProperty(LOGS_REPORT, PASS).get());
                final String environment = input.getStringAdditionalInformation(ENVIRONMENT).get();
                final String serviceName = input.getServiceName();

                properties.getPluginConfigurationProperty(LOGS_REPORT, "indexes").ifPresent(indexes -> {
                    for (Map.Entry<String, Object> index : ((Map<String, Object>) indexes).entrySet())
                    {
                        final LinkedHashMap<String, Object> indexConfig = (LinkedHashMap<String, Object>) index.getValue();
                        final String indexName = valueOf(indexConfig.get("name"));
                        final String payload =
                            replaceEach(valueOf(indexConfig.get("payload")), new String[] {"{ENVIRONMENT}", "{SERVICENAME}"}, new String[] {environment, serviceName});
                        final String requestUrl = format(valueOf(url), indexName);

                        ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl, POST, new HttpEntity<>(payload, headers(user, pass)), String.class);

                        boolean hasLogs = hasLogs(responseEntity);
                        input.setAdditionalInformation("hasLogs", hasLogs);

                        if (hasLogs)
                        {
                            break;
                        }
                    }
                });
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        });
        return input;
    }


    private boolean hasLogs(final ResponseEntity<String> responseEntity)
    {
        boolean hasLogs = false;
        try
        {
            if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody())
            {
                JsonNode node = mapper.readTree(responseEntity.getBody());
                hasLogs = node.get("hits").get("total").get("value").asInt() > 0;
            }
        }
        catch (Exception ex)
        {
            log.error("Could not parse response entity: {}", responseEntity.getBody(), ex);
        }
        return hasLogs;
    }


    private HttpHeaders headers(final String user, final String pass)
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(user, pass);
        httpHeaders.setContentType(APPLICATION_JSON);
        return httpHeaders;
    }
}

