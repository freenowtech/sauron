# ThanosAPI Report

## Description

This plugin is using the Thanos Query API to extract information about the services.

## Configuration
<pre>
- thanosapi-report:
  - baseUrl: BASE_URL           // the base url.
    - api:                      // Map of all the queries. 
      - NAME                    // Query name identifier.
        - query: URI            // Thanos query
          - fields:             // Fields to be extracted from the thanos response. 
            - KEY: VALUE        // Key is to be added to the `dataSet` and the value expression
</pre>

The value expression could be either [JsonPath](https://goessner.net/articles/JsonPath/) or [Spring Expression Language
(SpEL)](https://docs.spring.io/spring-framework/docs/4.3.12.RELEASE/spring-framework-reference/html/expressions.html) for evaluation.
In case of SpEL, You can still use the JsonPath as a part of the SpEL by using `#jsonPath(#body, "JsonPath expression goes here")

## How it works?

For each `query` from the plugin configuration, the plugin query Thanos by making an API call to `https://{host}/api/v1/query?query={query}
&dedup=true&partial_response=true`. For each key/value in the `fields` map, the plugin adds a new field by the name of the key in the dataSet by evaluating the key's value against 
the query response body.

The plugin replaces below parameters from the query:
- $environment: By the target environment. 
- $service: By the target service name. 

### Examples:

```
thanosapi-report:
    baseUrl: https://thanos.localhost.com
    api:
        request-per-minute: 
            query: sum(skipper:service_backend_requests_by_statuscode:1m{environment="$environment",service_name=~"$serviceName"})*60
            fields:
                rpm: $.data.result[0].value[1]
        circuit-breaker:
            query: sum(increase(hystrix_execution_total{application="$serviceName",environment=~"$environment"}[1m])) by (key) > 0
            fields:
                circuiteBreakerCommandKeysCount: $.data.result.length()
```

## Input
- `environment`: The target environment.
- `serviceName`: The service name.



## Output

- All keys in the API map will be added to the DataSet with the result of the evaluation of its value.