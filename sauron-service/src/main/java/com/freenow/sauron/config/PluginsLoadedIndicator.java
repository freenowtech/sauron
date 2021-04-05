package com.freenow.sauron.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
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


    public static void healthy()
    {
        healthy.compareAndExchange(false, true);
    }
}