package com.freenow.sauron.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig
{
    @Bean
    public RestTemplate restTemplate()
    {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setConnectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setReadTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        return new RestTemplate(httpRequestFactory);
    }
}