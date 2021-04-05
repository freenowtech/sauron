package com.freenow.sauron.config;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Collections;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.update.DefaultUpdateRepository;
import org.pf4j.update.UpdateManager;
import org.pf4j.update.UpdateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginUpdateConfig
{
    @Bean
    public PluginManager getPluginManager()
    {
        return new JarPluginManager();
    }


    @Bean
    @ConditionalOnMissingBean(UpdateRepository.class)
    public UpdateRepository updateRepository(PluginManager pluginManager) throws MalformedURLException
    {
        Path pluginsUrl = pluginManager.getPluginsRoot();
        return new DefaultUpdateRepository(DefaultUpdateRepository.class.getName(), pluginsUrl.toUri().toURL());
    }


    @Bean
    public UpdateManager getUpdateManager(PluginManager pluginManager, UpdateRepository repository)
    {
        return new UpdateManager(pluginManager, Collections.singletonList(repository));
    }
}