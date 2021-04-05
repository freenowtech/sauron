package com.freenow.sauron.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sauron.pipelines")
public class PipelineConfigurationProperties extends HashMap<String, List<String>>
{
    public List<String> getDefaultPipeline()
    {
        return getPipeline("default");
    }


    public List<String> getPipeline(String pipeline)
    {
        return this.getOrDefault(pipeline, Collections.emptyList());
    }
}
