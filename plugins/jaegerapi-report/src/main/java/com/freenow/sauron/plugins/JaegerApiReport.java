package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.jayway.jsonpath.JsonPath;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Extension
public class JaegerApiReport implements SauronExtension
{

    public static final String PLUGIN_ID = "jaegerapi-report";
    public static final String BASE_URL_CONFIG_PROPERTY = "baseUrl";
    public static final String API_CONFIG_PROPERTY = "api";
    public static final String URI_CONFIG_PROPERTY = "uri";
    public static final String FIELDS_CONFIG_PROPERTY = "fields";
    private static final String BODY_EVALUATION_CONTEXT_VARIABLE = "body";
    private final Method READ_METHOD = BeanUtils.resolveSignature("read(java.lang.String, java.lang.String, com.jayway.jsonpath.Predicate[])", JsonPath.class);
    private final RestTemplate restTemplate;
    private final ExpressionParser parser;
    private final StandardEvaluationContext evaluationContext;
    private String baseUrl;


    public JaegerApiReport()
    {
        parser = new SpelExpressionParser();
        evaluationContext = new StandardEvaluationContext();
        evaluationContext.registerFunction("jsonPath", READ_METHOD);

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setConnectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setReadTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        this.restTemplate = new RestTemplate(httpRequestFactory);
    }


    public JaegerApiReport(RestTemplate restTemplate)
    {
        parser = new SpelExpressionParser();
        evaluationContext = new StandardEvaluationContext();
        evaluationContext.registerFunction("jsonPath", READ_METHOD);

        this.restTemplate = restTemplate;
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {

        properties.getPluginConfigurationProperty(PLUGIN_ID, BASE_URL_CONFIG_PROPERTY).ifPresent(url ->
        {
            baseUrl = String.valueOf(url);
            try
            {
                properties.getPluginConfigurationProperty(PLUGIN_ID, API_CONFIG_PROPERTY)
                    .ifPresent(api -> ((Map<String, Map<String, Object>>) api)
                        .forEach((apiName, valuesMap) -> setScanPropertiesInformation(input, valuesMap)));
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        });
        return input;
    }


    private void setScanPropertiesInformation(final DataSet input, final Map<String, Object> valuesMap)
    {
        if (!valuesMap.containsKey(URI_CONFIG_PROPERTY) || !valuesMap.containsKey(FIELDS_CONFIG_PROPERTY))
        {
            return;
        }

        final String uri = (String) valuesMap.get(URI_CONFIG_PROPERTY);
        get(uri, input.getServiceName().toLowerCase()).ifPresent(jsonBody ->
            {
                evaluationContext.setVariable(BODY_EVALUATION_CONTEXT_VARIABLE, jsonBody);
                ((Map<String, String>) valuesMap.get(FIELDS_CONFIG_PROPERTY))
                    .forEach((key, expression) ->
                    {
                        try
                        {
                            Object result = evaluate(parser, evaluationContext, expression);
                            input.setAdditionalInformation(key, result);
                        }
                        catch (Exception ex)
                        {
                            log.debug("Can't evaluate [key: expression]: [{}: {}].", key, expression, ex);
                        }
                    });
            });
    }


    private Optional<String> get(final String uri, final String serviceName)
    {
        final String requestUrl = buildUrl(uri, serviceName);
        try
        {
            ResponseEntity<String> result = restTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            return Optional.ofNullable(result.getBody());
        }
        catch (Exception ex)
        {
            log.debug("Request: [{}] failed.", requestUrl, ex);
        }
        return Optional.empty();
    }


    private String buildUrl(final String uri, final String serviceName)
    {
        return (baseUrl + uri).replace("{serviceName}", serviceName);
    }


    private Object evaluate(final ExpressionParser parser, final StandardEvaluationContext evaluationContext, final String expression)
    {
        final Expression jsonExpression =
            expression.startsWith("$") ? parser.parseExpression(String.format("#jsonPath(#%s, '%s')", BODY_EVALUATION_CONTEXT_VARIABLE, expression)) :
                parser.parseExpression(expression);
        return jsonExpression.getValue(evaluationContext);
    }
}
