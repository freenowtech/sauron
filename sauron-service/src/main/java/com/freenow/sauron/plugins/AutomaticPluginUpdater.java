package com.freenow.sauron.plugins;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.pf4j.PluginRuntimeException;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AutomaticPluginUpdater
{
    private final UpdateManager updateManager;

    private final PluginManager pluginManager;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Autowired
    public AutomaticPluginUpdater(UpdateManager updateManager, PluginManager pluginManager, ApplicationEventPublisher applicationEventPublisher)
    {
        this.updateManager = updateManager;
        this.pluginManager = pluginManager;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostConstruct
    public void init()
    {
        log.info("Starting automatic plugin updater...");
        update();
        log.info("Plugins have been loaded.");
    }

    @Scheduled(cron = "${plugin.update.scheduler.cron}")
    public void update()
    {
        PluginsLoadedEvent event = new PluginsLoadedEvent();

        try
        {
            log.debug("Searching plugins in plugin repository...");

            updateManager.refresh();

            updateManager.getUpdates().parallelStream().forEach(this::updatePlugin);

            updateManager.getAvailablePlugins().parallelStream().forEach(this::installPlugin);

            event.setSuccess(true);

            applicationEventPublisher.publishEvent(event);
        }
        catch (Exception e)
        {
            log.error("Cannot load plugins '{}'", e.getMessage(), e);
        }
    }


    public void forceReload(String pluginId)
    {
        try
        {
            pluginManager.deletePlugin(pluginId);
            update();
        }
        catch (Exception e)
        {
            log.error("Cannot force update plugin {}. '{}'", pluginId, e.getMessage(), e);
        }
    }


    private void installPlugin(PluginInfo plugin)
    {
        try
        {
            String lastVersion = updateManager.getLastPluginRelease(plugin.id).version;
            log.info("Installing plugin '{}' with version {}", plugin.id, lastVersion);
            if (updateManager.installPlugin(plugin.id, lastVersion))
            {
                log.info("Installed plugin '{}'", plugin.id);
            }
            else
            {
                log.error("Cannot install plugin '{}'", plugin.id);
            }
        }
        catch (PluginRuntimeException e)
        {
            log.error("Cannot install plugin '{}'", e.getMessage(), e);
        }
    }


    private void updatePlugin(PluginInfo plugin)
    {
        try
        {
            String lastVersion = updateManager.getLastPluginRelease(plugin.id).version;
            String installedVersion = pluginManager.getPlugin(plugin.id).getDescriptor().getVersion();
            log.info("Updating plugin '{}' from version {} to version {}", plugin.id, installedVersion, lastVersion);

            if (updateManager.updatePlugin(plugin.id, lastVersion))
            {
                log.info("Updated plugin '{}'", plugin.id);
            }
            else
            {
                log.error("Cannot update plugin '{}'", plugin.id);
            }
        }
        catch (PluginRuntimeException e)
        {
            log.error("Cannot update plugin '{}'", e.getMessage(), e);
        }
    }
}