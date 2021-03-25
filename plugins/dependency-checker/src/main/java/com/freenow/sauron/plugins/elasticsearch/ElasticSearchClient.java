package com.freenow.sauron.plugins.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

@Slf4j
public class ElasticSearchClient
{
    private final RestClientBuilder builder;


    public ElasticSearchClient(PluginsConfigurationProperties properties)
    {
        builder = properties.getPluginConfigurationProperty("dependency-checker", "elasticsearch")
            .map(Map.class::cast)
            .map(elasticsearchConfig ->
            {
                String elasticsearchHost = (String) elasticsearchConfig.getOrDefault("host", "localhost");
                Integer elasticsearchPort = (Integer) elasticsearchConfig.getOrDefault("port", 9200);
                String elasticsearchProtocol = (String) elasticsearchConfig.getOrDefault("scheme", "https");

                return RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchProtocol));
            })
            .orElse(null);
    }


    public void index(DependenciesModel dependenciesModel)
    {
        if (builder != null)
        {
            try (RestHighLevelClient client = new RestHighLevelClient(builder))
            {
                IndexRequest request = getDocIndexRequest(dependenciesModel);
                IndexResponse response = client.index(request, RequestOptions.DEFAULT);

                RestStatus status = response.status();
                if (!status.equals(RestStatus.OK) && !status.equals(RestStatus.CREATED))
                {
                    log.error(String.format("Error [%s] storing document: %s", status, dependenciesModel.toJson()));
                }
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
            }
        }
    }


    private IndexRequest getDocIndexRequest(DependenciesModel model) throws JsonProcessingException
    {
        String indexName = String.format("dependencies-%s", LocalDate.now().getYear());
        IndexRequest request = new IndexRequest(indexName);
        request.source(model.toJson(), XContentType.JSON);
        return request;
    }
}