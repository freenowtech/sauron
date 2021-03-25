package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import org.pf4j.Extension;

@Extension
public class DataSanitizer implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.setAdditionalInformation("sanitizedServiceName", sanitizeString(input.getServiceName()));
        input.setAdditionalInformation("sanitizedOwner", sanitizeString(input.getOwner()));

        return input;
    }


    private String sanitizeString(String dirty)
    {
        return dirty.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
    }
}