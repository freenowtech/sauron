# JaegerAPI Report

## Description

This plugin is to use Jaeger API to extract data.

## Configuration
<pre>
- jaegerapi-report:
  - baseUrl: BASE_URL           // the base url.
    - api:                      // Map of all the api URI. 
      - NAME                    // API uri identifier.
        - uri: URI              // Sonar endpoint URI
          - fields:             // Fields to be extracted from the sonar response. 
            - KEY: VALUE        // Key is to be added to the `dataSet` and the value expression
</pre>

The value expression could be either [JsonPath](https://goessner.net/articles/JsonPath/) or [Spring Expression Language
(SpEL)](https://docs.spring.io/spring-framework/docs/4.3.12.RELEASE/spring-framework-reference/html/expressions.html) for evaluation.
In case of SpEL, You can still use the JsonPath as a part of the SpEL by using `#jsonPath(#body, "JsonPath expression goes here")
### Examples:

```
jaegerapi-report:
    baseUrl: https://jaegerui.localhost.com
    api:
        services-operations: 
            uri: /api/services/{serviceName}/operations
            fields:
                jaeger_enabled: "#jsonPath(#body, '$.total') > 0"
```

## Input

- `serviceName`: The service name. 


## Output

- All keys in the API map will be added to the DataSet with the result of the evaluation of its value.