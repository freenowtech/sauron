package com.freenow.sauron.datatransferobject;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildRequest implements Serializable
{
    @NotNull
    @NotEmpty
    private String serviceName;

    @NotNull
    @NotEmpty
    private String repositoryUrl;

    @NotNull
    @NotEmpty
    private String commitId;

    @NotNull
    @NotEmpty
    private String buildId;

    @NotNull
    @NotEmpty
    private String owner;

    @NotNull
    private Date eventTime;

    @NotNull
    @NotEmpty
    private String environment;

    @NotNull
    @NotEmpty
    private String release;

    @NotNull
    @NotEmpty
    private String dockerImage;

    @NotNull
    private Integer returnCode;

    @NotNull
    private Boolean rollback;

    @Builder.Default
    private String user = "none";

    @ApiModelProperty(required = false)
    @NotNull
    @Builder.Default
    private Platform platform = Platform.ECS;

    @ApiModelProperty(required = false)
    @NotNull
    @Builder.Default
    private DeploymentStrategy deploymentStrategy = DeploymentStrategy.BLUE_GREEN;

    @ApiModelProperty(required = false)
    private String documentId;

    @ApiModelProperty(required = false)
    private String indexName;

    @ApiModelProperty(required = false)
    private String plugin;

    @ApiModelProperty(required = false)
    private String customizedRepositoryUrl;

    @ApiModelProperty(required = false)
    private Integer deploymentState;

    @ApiModelProperty(required = false)
    private Integer tookSeconds;

    @ApiModelProperty(required = false)
    private Integer canaryWeight;


    public BuildRequest setUser(String user)
    {
        this.user = StringUtils.isEmpty(user) ? "none" : user;
        return this;
    }


    public enum DeploymentStrategy
    {
        AUTO_CANARY,
        BLUE_GREEN,
        CANARY,
        CRON,
        ROLLING,
        OTHER
    }

    public enum Platform
    {
        ECS,
        K8S
    }
}
