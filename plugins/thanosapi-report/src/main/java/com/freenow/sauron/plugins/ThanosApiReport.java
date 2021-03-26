package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Extension
public class ThanosApiReport implements SauronExtension
{
    private static final String PLUGIN_ID = "thanosapi-report";
    private static final String BASE_URL_CONFIG_PROPERTY = "baseUrl";
    private static final String API_CONFIG_PROPERTY = "api";
    private static final String QUERY_CONFIG_PROPERTY = "query";
    private static final String FIELDS_CONFIG_PROPERTY = "fields";
    private static final String BODY_EVALUATION_CONTEXT_VARIABLE = "body";
    private static final String API_QUERY_URI = "%s/api/v1/query?query=%s&dedup=true&partial_response=true";
    private static final String STATUS_EXPRESSION = "$.status";
    private static final String SUCCESS = "success";
    private static final String JSON_PATH_FUNCTION_NAME = "jsonPath";
    private static final Method READ_METHOD = BeanUtils.resolveSignature("read(java.lang.String, java.lang.String, com.jayway.jsonpath.Predicate[])", JsonPath.class);
    private final RestTemplate restTemplate;
    private final ExpressionParser parser;
    private final StandardEvaluationContext evaluationContext;


    public ThanosApiReport()
    {
        parser = new SpelExpressionParser();
        evaluationContext = new StandardEvaluationContext();
        evaluationContext.registerFunction(JSON_PATH_FUNCTION_NAME, READ_METHOD);

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setConnectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        httpRequestFactory.setReadTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(5)));
        this.restTemplate = new RestTemplate(httpRequestFactory);
    }


    public ThanosApiReport(RestTemplate restTemplate)
    {
        parser = new SpelExpressionParser();
        evaluationContext = new StandardEvaluationContext();
        evaluationContext.registerFunction(JSON_PATH_FUNCTION_NAME, READ_METHOD);

        this.restTemplate = restTemplate;
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {

        final String environment = input.getStringAdditionalInformation("environment").orElse(null);
        final String baseUrl = (String) properties.getPluginConfigurationProperty(PLUGIN_ID, BASE_URL_CONFIG_PROPERTY).orElse(null);
        final Map<String, Map<String, Object>> queryMap = (Map<String, Map<String, Object>>) properties.getPluginConfigurationProperty(PLUGIN_ID, API_CONFIG_PROPERTY).orElse(null);
        if (!StringUtils.hasLength(environment) || !StringUtils.hasLength(baseUrl) || CollectionUtils.isEmpty(queryMap))
        {
            return input;
        }

        try
        {
            queryMap.forEach((queryName, valuesMap) ->
                {
                    if (valuesMap.containsKey(QUERY_CONFIG_PROPERTY) && valuesMap.containsKey(FIELDS_CONFIG_PROPERTY))
                    {
                        final String configuredQuery = (String) valuesMap.get(QUERY_CONFIG_PROPERTY);
                        get(baseUrl, configuredQuery, environment, input.getServiceName())
                            .ifPresent(jsonBody ->
                            {
                                evaluationContext.setVariable(BODY_EVALUATION_CONTEXT_VARIABLE, jsonBody);
                                if (SUCCESS.equalsIgnoreCase((String) evaluate(parser, evaluationContext, STATUS_EXPRESSION)))
                                {
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
                                                log.debug("Can't evaluate [key: expression]: [{}: {}]. configuredQuery: [{}], responseBody: [{}]", key, expression, configuredQuery
                                                    , jsonBody,
                                                    ex);
                                            }
                                        });
                                }
                            });
                    }
                }
            );
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }

        return input;
    }


    private Optional<String> get(final String baseUrl, final String configuredQuery, final String environment, final String serviceName)
    {
        try
        {
            final String requestUrl = buildUrl(baseUrl, configuredQuery, environment, serviceName);
            String result = restTemplate.getForObject(URI.create(requestUrl), String.class);
            return Optional.ofNullable(result);
        }
        catch (Exception ex)
        {
            log.debug("Request failed: {}.", ex.getMessage(), ex);
        }
        return Optional.empty();
    }


    private String buildUrl(final String baseUrl, final String configuredQuery, final String environment, final String serviceName) throws UnsupportedEncodingException
    {
        final String query = configuredQuery.replace("$serviceName", serviceName).replace("$environment", environment);
        return String.format(API_QUERY_URI, baseUrl, URLEncoder.encode(query, StandardCharsets.UTF_8));
    }


    private Object evaluate(final ExpressionParser parser, final StandardEvaluationContext evaluationContext, final String expression)
    {
        final Expression jsonExpression =
            expression.startsWith("$") ? parser.parseExpression(String.format("#%s(#%s, '%s')", JSON_PATH_FUNCTION_NAME, BODY_EVALUATION_CONTEXT_VARIABLE, expression)) :
                parser.parseExpression(expression);
        return jsonExpression.getValue(evaluationContext);
    }
}
