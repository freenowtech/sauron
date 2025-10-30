package com.freenow.sauron.properties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sauron")
public class PipelineConfigurationProperties
{
    //A map of pipeline names to the list of plugin IDs that comprise them.Spring Boot will bind properties like `sauron.pipelines.default` into this map.
    private Map<String, List<String>> pipelines = Collections.emptyMap();

    //The ID of the plugin that must be executed at the end of a user-defined pipeline run.
    private String mandatoryOutputPlugin = "elasticsearch-output";

    public List<String> getDefaultPipeline()
    {
        return getPipeline("default");
    }

    public List<String> getPipeline(String pipeline)
    {
        return pipelines.getOrDefault(pipeline, Collections.emptyList());
    }
}
