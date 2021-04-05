package com.freenow.sauron.config;

import com.freenow.sauron.plugins.artifactory.ArtifactoryDownloader;
import com.freenow.sauron.plugins.artifactory.ArtifactoryRepository;
import com.freenow.sauron.properties.ArtifactoryConfigurationProperties;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.pf4j.update.UpdateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(ArtifactoryConfigurationProperties.class)
@ConditionalOnProperty(name = "sauron.artifactory.enabled", havingValue = "true")
public class ArtifactoryConfig
{
    @Bean
    public Artifactory artifactoryClient(ArtifactoryConfigurationProperties artifactoryConfigurationProperties)
    {
        return ArtifactoryClientBuilder.create()
            .setUrl(artifactoryConfigurationProperties.getUrl())
            .setUsername(artifactoryConfigurationProperties.getUsername())
            .setPassword(artifactoryConfigurationProperties.getPassword())
            .build();
    }

    @Bean
    public ArtifactoryDownloader artifactoryDownloader(Artifactory artifactory)
    {
        return new ArtifactoryDownloader(artifactory);
    }


    @Bean
    @Primary
    public UpdateRepository artifactoryRepository(
        Artifactory artifactory,
        ArtifactoryDownloader artifactoryDownloader,
        ArtifactoryConfigurationProperties properties)
    {
        return new ArtifactoryRepository(artifactory, artifactoryDownloader, properties);
    }
}