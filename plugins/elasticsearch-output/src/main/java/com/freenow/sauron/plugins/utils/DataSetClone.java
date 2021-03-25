package com.freenow.sauron.plugins.utils;

import com.freenow.sauron.model.DataSet;
import java.util.Arrays;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DataSetClone
{

    public static DataSet removeAdditionalInformation(DataSet dataSet, String... keys)
    {
        Map<String, Object> additionalInformation = dataSet.copyAdditionalInformation();
        Arrays.stream(keys).forEach(additionalInformation::remove);

        return DataSet
            .builder()
            .buildId(dataSet.getBuildId())
            .commitId(dataSet.getCommitId())
            .eventTime(dataSet.getEventTime())
            .owner(dataSet.getOwner())
            .repositoryUrl(dataSet.getRepositoryUrl())
            .serviceName(dataSet.getServiceName())
            .additionalInformation(additionalInformation)
            .build();
    }
}
