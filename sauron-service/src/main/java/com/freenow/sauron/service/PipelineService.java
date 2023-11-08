package com.freenow.sauron.service;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import com.freenow.sauron.mapper.BuildMapper;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.SauronExtension;
import com.freenow.sauron.properties.PipelineConfigurationProperties;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableConfigurationProperties({PipelineConfigurationProperties.class, PluginsConfigurationProperties.class})
public class PipelineService
{
    private static final String ELASTICSEARCH_OUTPUT_PLUGIN = "elasticsearch-output";

    private final PipelineConfigurationProperties pipelineProperties;

    private final PluginsConfigurationProperties pluginsProperties;

    private final PluginManager pluginManager;

    private final RequestHandler handler;


    @Autowired
    public PipelineService(
        PluginManager pluginManager,
        PipelineConfigurationProperties pipelineProperties,
        PluginsConfigurationProperties pluginsProperties,
        RequestHandler handler)
    {
        this.pluginManager = pluginManager;
        this.pipelineProperties = pipelineProperties;
        this.pluginsProperties = pluginsProperties;
        this.handler = handler;
        handler.setConsumer(this::process);
    }


    public void publish(BuildRequest request)
    {
        try
        {
            handler.handle(request);
        }
        catch (Exception ex)
        {
            log.error("Error publishing request to be processed.", ex);
        }
    }


    void process(BuildRequest request)
    {
        try
        {
            final DataSet dataSet = BuildMapper.makeDataSet(request);
            String plugin = request.getPlugin();

            if (StringUtils.isNotBlank(plugin))
            {
                plugin = StringUtils.lowerCase(request.getPlugin());
                final List<String> defaultPipeline = pipelineProperties.getDefaultPipeline();

                log.debug("Running user defined pipeline.");

                if (defaultPipeline.contains(plugin))
                {
                    runDependencies(request, dataSet, plugin, defaultPipeline);
                }

                runPlugin(plugin, request, dataSet);
                runPlugin(ELASTICSEARCH_OUTPUT_PLUGIN, request, dataSet);
            }
            else
            {
                log.debug("Running default pipeline.");
                pipelineProperties.getDefaultPipeline().forEach(pluginId -> runPlugin(pluginId, request, dataSet));
            }
        }
        catch (final Exception ex)
        {
            log.error(String.format("Error loading plugins: %s", ex.getMessage()), ex);
        }
    }


    private void runDependencies(
        final BuildRequest request, final DataSet dataSet,
        final String plugin, final List<String> defaultPipeline)
    {
        for (final String defaultPipelinePlugin : defaultPipeline)
        {
            if (StringUtils.equals(plugin, defaultPipelinePlugin))
            {
                return;
            }

            runPlugin(defaultPipelinePlugin, request, dataSet);
        }
    }


    void runPlugin(String plugin, BuildRequest request, DataSet dataSet)
    {
        pluginManager.getExtensions(SauronExtension.class, plugin).forEach(pluginExtension -> {
            try
            {
                log.debug(String.format("Applying pluginId: %s. Processing service %s - %s", plugin, request.getServiceName(), request.getCommitId()));
                MDC.put("sauron.pluginId", plugin);
                MDC.put("sauron.serviceName", request.getServiceName());
                MDC.put("sauron.commitId", request.getCommitId());
                pluginExtension.apply(pluginsProperties, dataSet);
            }
            catch (final Exception ex)
            {
                log.error(String.format("Error processing pipeline: %s:%s. %s", request.getServiceName(), request.getCommitId(), ex.getMessage()), ex);
            }
        });
    }
}
