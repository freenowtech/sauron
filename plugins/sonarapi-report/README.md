# SonarAPI Report

## Description

This plugin is to use Sonar API to extract data. 

## Configuration
<pre>
- sonarapi-report:
  - baseUrl: BASE_URL           // the base url.
  - accessToken: ACCESS_TOKEN   // the ciphered access token. 
    - api:                      // Map of all the api URI. 
      - NAME                    // API uri identifier.
        - uri: URI              // Sonar endpoint URI
          - fields:             // Fields to be extracted from the sonar response. 
            - KEY: VALUE        // Key is to be added to the `dataSet` and the value expression in [JsonPath](https://goessner.net/articles/JsonPath/)
</pre>
### Examples:

```
sonarapi-report:
    baseUrl: https://sonar.localhost.com
    accessToken: "{cipher}xxxx"
    api:
        measure-component: 
            uri: /api/measures/component?component=?&metricKeys=coverage
            fields:
                code_coverage: $.component.measures[?(@.metric=='coverage')].value
```

## Input

- `serviceName`: The service name.


## Output

- All keys in the API map will be added to the DataSet with the result of the evaluation of its value.