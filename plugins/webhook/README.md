# Webhook Plugin

## Description

This plugin sends HTTP requests to third-party endpoints to trigger further processing.

## Configuration

```yaml
- webhook:
    endpoints:
      - url: "http://example.localhost/trigger" # URL to send the request to. Required.
        method: "POST"                          # HTTP method to use. Default is POST.
        includeDataSet: false                   # Send the Sauron Data Set as the body of the request. Default is false.
```

## Input

This plugin doesn't require specific input keys to be set.

## Output

This plugin doesn't modify the Sauron Data Set. It returns the Data Set as is.
