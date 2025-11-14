package com.freenow.sauron.service;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import com.freenow.sauron.mapper.BuildMapper;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.SauronExtension;
import com.freenow.sauron.properties.PipelineConfigurationProperties;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final PipelineConfigurationProperties pipelineProperties;

    private final PluginsConfigurationProperties pluginsProperties;

    private final PluginManager pluginManager;

    private final RequestHandler handler;

    private final MeterRegistry meterRegistry;


    @Autowired
    public PipelineService(
        PluginManager pluginManager,
        PipelineConfigurationProperties pipelineProperties,
        PluginsConfigurationProperties pluginsProperties,
        RequestHandler handler,
        MeterRegistry meterRegistry)
    {
        this.pluginManager = pluginManager;
        this.pipelineProperties = pipelineProperties;
        this.pluginsProperties = pluginsProperties;
        this.handler = handler;
        this.meterRegistry = meterRegistry;
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
            DataSet dataSet = BuildMapper.makeDataSet(request);
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
                    runDependencies(dataSet, plugin, defaultPipeline);
                }

                log.debug("Executing user-defined plugin: {}", plugin);
                runPlugin(plugin, dataSet);

                String mandatoryOutputPlugin = pipelineProperties.getMandatoryOutputPlugin();
                if (StringUtils.isNotBlank(mandatoryOutputPlugin)) {
                    log.debug("Executing mandatory output plugin: {}", mandatoryOutputPlugin);
                    runPlugin(mandatoryOutputPlugin, dataSet);
                }
            }
            else
            {
                log.debug("No user-defined plugin. Running default pipeline. Default pipeline plugins: {}", pipelineProperties.getDefaultPipeline());
                pipelineProperties.getDefaultPipeline().forEach(pluginId -> runPlugin(pluginId, dataSet));
            }
        }
        catch (final Exception ex)
        {
            log.error("Error processing request for serviceName={}, commitId={}", request.getServiceName(), request.getCommitId(), ex);
        }
    }


    private void runDependencies(
        DataSet dataSet, final String plugin, final List<String> defaultPipeline)
    {
        for (final String defaultPipelinePlugin : defaultPipeline)
        {
            if (StringUtils.equals(plugin, defaultPipelinePlugin))
            {
                log.debug("Dependency plugin '{}' is the main plugin '{}', skipping further dependencies.", defaultPipelinePlugin, plugin);
                return;
            }

            log.debug("Running dependency plugin: {} for main plugin: {}", defaultPipelinePlugin, plugin);
            runPlugin(defaultPipelinePlugin, dataSet);
        }
    }


    void runPlugin(String plugin, DataSet dataSet)
    {
        for (SauronExtension pluginExtension : pluginManager.getExtensions(SauronExtension.class, plugin))
        {
            try
            {
                MDC.put("sauron.pluginId", plugin);
                MDC.put("sauron.serviceName", dataSet.getServiceName());
                MDC.put("sauron.commitId", dataSet.getCommitId());
                MDC.put("sauron.buildId", dataSet.getBuildId());
                log.debug("Applying pluginId: {}. Processing service {} - {}. DataSet BEFORE plugin execution: {}", plugin, dataSet.getServiceName(), dataSet.getCommitId(),
                    dataSet);

                getTimerBuilder("sauron.plugin.execution.time")
                    .tag("plugin", plugin)
                    .register(meterRegistry).record(() -> pluginExtension.apply(pluginsProperties, dataSet));

                meterRegistry.counter("sauron.plugin.executions.total", "plugin", plugin, "result", "success").increment();
                log.debug("PluginId: {} applied. Processing service {} - {}. DataSet AFTER plugin execution: {}", plugin, dataSet.getServiceName(), dataSet.getCommitId(), dataSet);
            }
            catch (final Exception ex)
            {
                meterRegistry.counter("sauron.plugin.executions.total", "plugin", plugin, "result", "failure").increment();
                log.error("Error in plugin '{}' for serviceName={}, commitId={}. DataSet at time of failure: {}", plugin, dataSet.getServiceName(), dataSet.getCommitId(), dataSet, ex);
            }
            finally
            {
                MDC.remove("sauron.pluginId");
                MDC.remove("sauron.serviceName");
                MDC.remove("sauron.commitId");
                MDC.remove("sauron.buildId");
            }
        }
    }


    Timer.Builder getTimerBuilder(String name)
    {
        return Timer.builder(name);
    }
}
