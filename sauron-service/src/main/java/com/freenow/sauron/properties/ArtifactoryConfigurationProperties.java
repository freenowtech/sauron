package com.freenow.sauron.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("sauron.artifactory")
public class ArtifactoryConfigurationProperties
{
    private String url;

    private String repository;

    private String username;

    private String password;

    private String groupId;

    private Boolean enabled;
}
