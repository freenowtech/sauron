#!/usr/bin/env bash

curl -X PUT \
  http://localhost:9200/_template/sauron-template \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
    "index_patterns":[
        "sauron*"
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
                "limit":"10000"
            }
        },
        "refresh_interval":"30s",
        "number_of_shards":"1",
        "number_of_replicas":"1"
    },
    "mappings":{
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
              "environment": {"type": "keyword"},
              "release": {"type": "keyword"},
              "user": {"type": "keyword"},
              "asgard": {"type": "keyword"},
              "django": {"type": "keyword"},
              "eddy": {"type": "keyword"},
              "elisabeth": {"type": "keyword"},
              "gracia": {"type": "keyword"},
              "jeffrey": {"type": "keyword"},
              "johnny": {"type": "keyword"},
              "live": {"type": "keyword"},
              "mgmt": {"type": "keyword"},
              "pamela": {"type": "keyword"},
              "payout": {"type": "keyword"},
              "pooling": {"type": "keyword"},
              "prelive": {"type": "keyword"},
              "sandbox": {"type": "keyword"},
              "sophie": {"type": "keyword"}
        }
    }
}'
