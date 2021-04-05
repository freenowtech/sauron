package com.freenow.sauron.service;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.model.DataSet;
import java.time.Instant;
import java.util.Date;

abstract class UtilsBaseTest
{
    private final String SERVICE_NAME = "test";

    private final String REPOSITORY_URL = "git://test.com";

    private final String COMMIT_ID = "1";

    private final String BUILD_ID = "2";

    private final String OWNER = "sauron";

    private final Date EVENT_TIME = Date.from(Instant.EPOCH);

    private final String ENVIRONMENT = "environment";

    private final String RELEASE = "release-1";

    private final String DOCKER_IMAGE = "docker.it/test:1.0";

    private final Integer RETURN_CODE = 0;

    private final Boolean ROLLBACK = false;

    private final String USER = "sauron-service";

    private final String DEPLOYMENT_STRATEGY = "BLUE_GREEN";

    private final String PLATFORM = "ECS";

    private final String DOCUMENT_ID = "ab1cd2de3";

    private final String INDEX_NAME = "sauron-12";

    DataSet dataSet()
    {
        DataSet dataSet = DataSet.builder()
            .serviceName(SERVICE_NAME)
            .repositoryUrl(REPOSITORY_URL)
            .commitId(COMMIT_ID)
            .buildId(BUILD_ID)
            .owner(OWNER)
            .eventTime(EVENT_TIME)
            .build();

        dataSet.setAdditionalInformation("environment", ENVIRONMENT);
        dataSet.setAdditionalInformation("release", RELEASE);
        dataSet.setAdditionalInformation("dockerImage", DOCKER_IMAGE);
        dataSet.setAdditionalInformation("returnCode", RETURN_CODE);
        dataSet.setAdditionalInformation("rollback", ROLLBACK);
        dataSet.setAdditionalInformation("user", USER);
        dataSet.setAdditionalInformation("deploymentStrategy", DEPLOYMENT_STRATEGY);
        dataSet.setAdditionalInformation("platform", PLATFORM);
        dataSet.setAdditionalInformation("documentId", DOCUMENT_ID);
        dataSet.setAdditionalInformation("indexName", INDEX_NAME);

        return dataSet;
    }


    BuildRequest buildRequest()
    {
        return BuildRequest.builder()
            .serviceName(SERVICE_NAME)
            .repositoryUrl(REPOSITORY_URL)
            .commitId(COMMIT_ID)
            .buildId(BUILD_ID)
            .documentId(DOCUMENT_ID)
            .indexName(INDEX_NAME)
            .owner(OWNER)
            .eventTime(EVENT_TIME)
            .environment(ENVIRONMENT)
            .release(RELEASE)
            .dockerImage(DOCKER_IMAGE)
            .returnCode(RETURN_CODE)
            .rollback(ROLLBACK)
            .user(USER)
            .build();
    }
}
