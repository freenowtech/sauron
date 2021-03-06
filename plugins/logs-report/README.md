# Plugin Logs Report

## Description

This plugin will tell if your service has any logs on elasticsearch, in the configured indexes, within the range specified in the request 
payload (default last 10min).

## Configuration
<pre>
- logs-report:
  - url:                            // the base url to elasticsearch.
  - user:                           // the api user. 
  - password:                       // the api password.
  - indexes:                        // indexes will be used to search for logs (it's a findFirst search)
      name:                         // index name, like logstash-*
      payload: "{request-payload}"  // payload used in the search for logs  
</pre>


### Examples:

```
logs-report:
  url: https://elasticsearch-logs.localhost.com:443/{index-name}/_search
  user: "kibana"
  password: "xxxx"
  indexes:
    -
      name: "logstash-*"
      payload: "{"query":{"bool":{"must":[{"query_string":{"analyze_wildcard":true,"query":"@tags: {ENVIRONMENT} AND @source: {SERVICENAME}"}}],"filter":[{"range":{"@timestamp":{"format":"basic_date_time","gte":"now-10m","lt":"now"}}}]}}}" 
    -
      name: "filebeat-*"
      payload: "{"query":{"bool":{"filter":[{"match_phrase":{"environment":{"query":"{ENVIRONMENT}"}}},{"match_phrase":{"service.name":{"query":"{SERVICENAME}"}}},{"range":{"@timestamp":{"format":"basic_date_time","gte":"now-10m","lt":"now"}}}]}}}"    
```

## Input

Mandatory input keys in
[DataSet](https://github.com/freenowtech/sauron/blob/main/core/src/main/java/com/freenow/sauron/model/DataSet.java)
object required by this plugin:

- `serviceName:` The service name. It will be used as filter (@source) on elasticsearch.
- `environment:` The environment name. It will be used as filter (@tags) on elasticsearch.


## Output

The output key that is produced in
[DataSet](https://github.com/freenowtech/sauron/blob/main/core/src/main/java/com/freenow/sauron/model/DataSet.java)
object.

- `hasLogs: true/false` If the service has logs in one of those indexes `true` otherwise `false`.