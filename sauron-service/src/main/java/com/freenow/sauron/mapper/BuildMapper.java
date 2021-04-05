package com.freenow.sauron.mapper;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.model.DataSet;
import java.util.Optional;
import org.modelmapper.ModelMapper;

public final class BuildMapper
{
    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    private BuildMapper()
    {
        throw new UnsupportedOperationException();
    }


    public static DataSet makeDataSet(BuildRequest request)
    {
        DataSet dataSet = MODEL_MAPPER.map(request, DataSet.class);
        Optional.ofNullable(request.getEnvironment()).ifPresent(environment -> dataSet.setAdditionalInformation("environment", environment));
        Optional.ofNullable(request.getRelease()).ifPresent(release -> dataSet.setAdditionalInformation("release", release));
        Optional.ofNullable(request.getDockerImage()).ifPresent(dockerImage -> dataSet.setAdditionalInformation("dockerImage", dockerImage));
        Optional.ofNullable(request.getReturnCode()).ifPresent(returnCode -> dataSet.setAdditionalInformation("returnCode", returnCode));
        Optional.ofNullable(request.getRollback()).ifPresent(rollback -> dataSet.setAdditionalInformation("rollback", rollback));
        Optional.ofNullable(request.getUser()).ifPresent(user -> dataSet.setAdditionalInformation("user", user));
        Optional.ofNullable(request.getPlatform()).ifPresent(platform -> dataSet.setAdditionalInformation("platform", platform.toString()));
        Optional.ofNullable(request.getDeploymentStrategy()).ifPresent(deploymentStrategy -> dataSet.setAdditionalInformation("deploymentStrategy", deploymentStrategy.toString()));
        Optional.ofNullable(request.getDocumentId()).ifPresent(oldDocumentId -> dataSet.setAdditionalInformation("documentId", oldDocumentId));
        Optional.ofNullable(request.getIndexName()).ifPresent(indexName -> dataSet.setAdditionalInformation("indexName", indexName));
        Optional.ofNullable(request.getCustomizedRepositoryUrl()).ifPresent(customizedRepositoryUrl -> dataSet.setAdditionalInformation("customizedRepositoryUrl", customizedRepositoryUrl));
        Optional.ofNullable(request.getDeploymentState()).ifPresent(deploymentState -> dataSet.setAdditionalInformation("deploymentState", deploymentState));
        Optional.ofNullable(request.getTookSeconds()).ifPresent(tookSeconds -> dataSet.setAdditionalInformation("tookSeconds", tookSeconds));
        Optional.ofNullable(request.getCanaryWeight()).ifPresent(canaryWeight -> dataSet.setAdditionalInformation("canaryWeight", canaryWeight));

        return dataSet;
    }
}
