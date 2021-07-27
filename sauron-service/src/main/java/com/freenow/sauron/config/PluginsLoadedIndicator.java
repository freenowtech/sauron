package com.freenow.sauron.config;

import com.freenow.sauron.plugins.PluginsLoadedEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PluginsLoadedIndicator implements HealthIndicator
{
    private static final AtomicBoolean healthy = new AtomicBoolean(false);


    @Override
    public Health health()
    {
        return healthy.get() ?
            Health.up().build() :
            Health.down().withDetail("Plugins Startup", "Plugins have not been loaded yet").build();
    }


    @EventListener
    public void onPluginLoadedEvent(PluginsLoadedEvent event)
    {
        healthy.set(event.isSuccess());
    }
}