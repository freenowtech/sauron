package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class DataSetUtilsTest
{
    private static final String SERVICE_NAME = "service";

    private static final String REPOSITORY_URL = "repository";

    private static final String OWNER = "owner";

    private static final Date EVENT_TIME = new Date();

    private static final String COMMIT_ID = "commit id";

    private static final String BUILD_ID = "build id";

    private static final String ADDITIONAL_PROPERTY_KEY = "id";

    private static final int ADDITIONAL_PROPERTY_VALUE = 1;


    @Test
    public void dataSetShouldNotHaveAdditionalProperties()
    {
        DataSet dataSet = DataSetClone.removeAdditionalInformation(newDataSet(), ADDITIONAL_PROPERTY_KEY);

        Assert.assertEquals(SERVICE_NAME, dataSet.getServiceName());
        Assert.assertEquals(REPOSITORY_URL, dataSet.getRepositoryUrl());
        Assert.assertEquals(OWNER, dataSet.getOwner());
        Assert.assertEquals(EVENT_TIME, dataSet.getEventTime());
        Assert.assertEquals(COMMIT_ID, dataSet.getCommitId());
        Assert.assertEquals(BUILD_ID, dataSet.getBuildId());
        Assert.assertEquals(-1, dataSet.getIntegerAdditionalInformation(ADDITIONAL_PROPERTY_KEY).orElse(-1).intValue());
    }


    private DataSet newDataSet()
    {
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put(ADDITIONAL_PROPERTY_KEY, ADDITIONAL_PROPERTY_VALUE);

        return DataSet
            .builder()
            .serviceName(SERVICE_NAME)
            .repositoryUrl(REPOSITORY_URL)
            .owner(OWNER)
            .eventTime(EVENT_TIME)
            .commitId(COMMIT_ID)
            .buildId(BUILD_ID)
            .additionalInformation(additionalProperties)
            .build();
    }

}
