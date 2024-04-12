package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WebhookTest
{
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

    @Test
    public void testNotifierApply()
    {
        Mockito.when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(Object.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        Webhook plugin = new Webhook(restTemplate);
        DataSet dataSet = new DataSet();
        plugin.apply(createPluginConfigurationProperties(null, false), dataSet);

        verify(restTemplate, times(1))
            .exchange(
                eq("https://backstage.localhost/events/sauron"),
                eq(HttpMethod.POST),
                eq(null),
                eq(Object.class)
            );
    }

    @Test
    public void testNotifierApply_customHttpMethod()
    {
        Mockito.when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(Object.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        Webhook plugin = new Webhook(restTemplate);
        DataSet dataSet = new DataSet();
        plugin.apply(createPluginConfigurationProperties("PUT", false), dataSet);

        verify(restTemplate, times(1))
            .exchange(
                eq("https://backstage.localhost/events/sauron"),
                eq(HttpMethod.PUT),
                eq(null),
                eq(Object.class)
            );
    }

    @Test
    public void testNotifierApply_includeDataSet()
    {
        Mockito.when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(Object.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        Webhook plugin = new Webhook(restTemplate);
        DataSet dataSet = new DataSet();
        dataSet.setServiceName("TestService");
        plugin.apply(createPluginConfigurationProperties("POST", true), dataSet);

        verify(restTemplate, times(1))
            .exchange(
                eq("https://backstage.localhost/events/sauron"),
                eq(HttpMethod.POST),
                eq(new HttpEntity<>(dataSet)),
                eq(Object.class)
            );
    }

    private PluginsConfigurationProperties createPluginConfigurationProperties(String method, Boolean includeDataSet)
    {
        Map<String, Object> endpoint = new HashMap<>();
        endpoint.put("url", "https://backstage.localhost/events/sauron");
        if (method != null)
        {
            endpoint.put("method", method);
        }

        if (includeDataSet)
        {
            endpoint.put("includeDataSet", includeDataSet);
        }

        return new PluginsConfigurationProperties()
        {{
            put("webhook", new HashMap<>()
            {{
                put("endpoints", new HashMap<>()
                {{
                    put("unittest", endpoint);
                }});
            }});
        }};
    }
}
