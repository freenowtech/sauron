#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import org.pf4j.Extension;

@Extension
public class ${className} implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        // PLUGIN LOGIC

        return input;
    }
}
