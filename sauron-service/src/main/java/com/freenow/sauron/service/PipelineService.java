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
            log.info("Received request to publish: serviceName={}, commitId={}", request.getServiceName(), request.getCommitId());
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
            log.info("Starting processing for request: serviceName={}, commitId={}", request.getServiceName(), request.getCommitId());
            final DataSet dataSet = BuildMapper.makeDataSet(request);
            log.debug("Initial DataSet created from request: {}", dataSet);
            String plugin = request.getPlugin();

            if (StringUtils.isNotBlank(plugin))
            {
                plugin = StringUtils.lowerCase(request.getPlugin());
                final List<String> defaultPipeline = pipelineProperties.getDefaultPipeline();

                log.debug("User-defined plugin specified: {}. Running user defined pipeline. Default pipeline plugins: {}", plugin, defaultPipeline);

                if (defaultPipeline.contains(plugin))
                {
                    log.debug("User-defined plugin '{}' is part of the default pipeline. Running dependencies first.", plugin);
                    runDependencies(request, dataSet, plugin, defaultPipeline);
                }

                log.debug("Executing user-defined plugin: {}", plugin);
                runPlugin(plugin, request, dataSet);
                log.debug("Executing mandatory output plugin: {}", ELASTICSEARCH_OUTPUT_PLUGIN);
                runPlugin(ELASTICSEARCH_OUTPUT_PLUGIN, request, dataSet);
            }
            else
            {
                log.debug("No user-defined plugin. Running default pipeline. Default pipeline plugins: {}", pipelineProperties.getDefaultPipeline());
                pipelineProperties.getDefaultPipeline().forEach(pluginId -> runPlugin(pluginId, request, dataSet));
            }
        }
        catch (final Exception ex)
        {
            log.error("Error processing request for serviceName={}, commitId={}", request.getServiceName(), request.getCommitId(), ex);
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
                log.debug("Dependency plugin '{}' is the main plugin '{}', skipping further dependencies.", defaultPipelinePlugin, plugin);
                return;
            }

            log.debug("Running dependency plugin: {} for main plugin: {}", defaultPipelinePlugin, plugin);
            runPlugin(defaultPipelinePlugin, request, dataSet);
        }
    }


    void runPlugin(String plugin, BuildRequest request, DataSet dataSet)
    {
        pluginManager.getExtensions(SauronExtension.class, plugin).forEach(pluginExtension -> {
            try
            {
                MDC.put("sauron.pluginId", plugin);
                MDC.put("sauron.serviceName", request.getServiceName());
                MDC.put("sauron.commitId", request.getCommitId());
                log.debug("Applying pluginId: {}. Processing service {} - {}. DataSet BEFORE plugin execution: {}", plugin, request.getServiceName(), request.getCommitId(), dataSet);
                
                long startTime = System.currentTimeMillis();
                pluginExtension.apply(pluginsProperties, dataSet);
                long duration = System.currentTimeMillis() - startTime;
                log.info("Plugin '{}' executed in {}ms", plugin, duration);
                log.debug("PluginId: {} applied. Processing service {} - {}. DataSet AFTER plugin execution: {}", plugin, request.getServiceName(), request.getCommitId(), dataSet);
            }
            catch (final Exception ex)
            {
                log.error("Error in plugin '{}' for serviceName={}, commitId={}", plugin, request.getServiceName(), request.getCommitId(), ex);
            }
            finally
            {
                MDC.remove("sauron.pluginId");
                MDC.remove("sauron.serviceName");
                MDC.remove("sauron.commitId");
            }
        });
    }
}
