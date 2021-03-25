package com.freenow.sauron.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.utils.DataSetClone;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.pf4j.Extension;
import org.pf4j.util.StringUtils;

@Extension
@Slf4j
public class ElasticsearchOutput implements SauronExtension
{
    private static final String DOCUMENT_ID_KEY = "documentId";

    private static final String INDEX_NAME_KEY = "indexName";


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        Map<String, Object> elasticsearchProperties = (Map) properties.getPluginConfigurationProperty("elasticsearch-output", "elasticsearch").orElseGet(HashMap::new);
        String elasticsearchHost = (String) elasticsearchProperties.getOrDefault("host", "localhost");
        Integer elasticsearchPort = (Integer) elasticsearchProperties.getOrDefault("port", 9200);
        String elasticsearchProtocol = (String) elasticsearchProperties.getOrDefault("scheme", "https");

        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchProtocol))))
        {
            String documentId = input.getStringAdditionalInformation(DOCUMENT_ID_KEY).orElse(null);
            String indexName = input.getStringAdditionalInformation(INDEX_NAME_KEY).orElse(null);
            DataSet inputClone = DataSetClone.removeAdditionalInformation(input, DOCUMENT_ID_KEY, INDEX_NAME_KEY);

            if (isUpdateDocument(documentId, indexName))
            {
                updateDocument(indexName, documentId, inputClone, client);
            }
            else
            {
                IndexRequest docRequest = getDocIndexRequest(inputClone);
                index(inputClone, client, docRequest);
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return input;
    }


    private boolean isUpdateDocument(String documentId, String indexName)
    {
        return StringUtils.isNotNullOrEmpty(documentId) && StringUtils.isNotNullOrEmpty(indexName);
    }


    private void updateDocument(String indexName, String documentId, DataSet input, RestHighLevelClient client) throws IOException
    {
        UpdateRequest updateRequest = new UpdateRequest(indexName, documentId).doc(input.toJson(), XContentType.JSON);
        UpdateResponse response = client.update(updateRequest, RequestOptions.DEFAULT);

        onNotSuccessfulRequest(response.status(), status -> log.error(String.format("Error [%s] updating document: %s-%s", status, input.getServiceName(), documentId)));
    }


    private void index(DataSet input, RestHighLevelClient client, IndexRequest request) throws IOException
    {
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        onNotSuccessfulRequest(response.status(), status -> log.error(String.format("Error [%s] storing document: %s-%s", status, input.getServiceName(), input.getCommitId())));
    }


    private void onNotSuccessfulRequest(RestStatus status, Consumer<RestStatus> consumer)
    {
        if (!status.equals(RestStatus.OK) && !status.equals(RestStatus.CREATED))
        {
            consumer.accept(status);
        }
    }


    private IndexRequest getDocIndexRequest(DataSet input) throws JsonProcessingException
    {
        String indexName = String.format("sauron-%s", LocalDate.now().getYear());
        IndexRequest request = new IndexRequest(indexName);
        request.source(input.toJson(), XContentType.JSON);
        return request;
    }
}
