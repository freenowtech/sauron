package com.freenow.sauron.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
public class ConsoleOutput implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        }
        catch (JsonProcessingException ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return input;
    }
}