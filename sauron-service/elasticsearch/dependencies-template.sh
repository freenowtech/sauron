#!/usr/bin/env bash

curl -X PUT \
  http://localhost:9200/_template/sauron-template \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
    "index_patterns":[
        "dependencies-*"
    ],
    "settings":{
        "index":{
            "analysis":{
                "analyzer":{
                    "analyzer_case_insensitive":{
                        "tokenizer":"keyword",
                        "filter":"lowercase"
                    }
                }
            }
        },
        "mapping":{
            "total_fields":{
                "limit":"20000"
            }
        },
        "refresh_interval":"30s",
        "number_of_shards":"1",
        "number_of_replicas":"1"
    },
    "mappings":{
          "dynamic_templates":[
              {
                  "maven_deps_as_keywords": {
                      "match_mapping_type":"string",
                      "match_pattern":"regex",
                      "match":"^.*:.*$",
                      "mapping":{
                          "type":"keyword",
                          "norms":false
                      }
                  }
              },
              {
                   "custom_versions_as_keyword": {
                      "match_mapping_type":"string",
                      "match_pattern":"regex",
                      "match":"^.*_version$",
                      "mapping":{
                          "type":"keyword",
                          "norms":false
                      }
                  }
              }
          ],
          "properties": {
              "serviceName": {
                  "type": "text",
                  "analyzer":"analyzer_case_insensitive",
                  "fields": { "keyword": { "type": "keyword", "ignore_above": 256 } }
              },
              "owner": {
                  "type": "text",
                  "analyzer":"analyzer_case_insensitive",
                  "fields": { "keyword": { "type": "keyword", "ignore_above": 256 } }
              },
              "buildId": {"type": "keyword"},
              "commitId": {"type": "keyword"},
              "environment": {"type": "keyword"}
        }
    }
}'
