package com.freenow.sauron.plugins.utils;

import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RetryCommand<T>
{
    private final RetryConfig config;


    // Takes a function and executes it, if fails, passes the function to the retry command
    public T run(Supplier<T> function)
    {
        try
        {
            return function.get();
        }
        catch (Exception ex)
        {
            log.warn("FAILED - Command failed, will be retried {} times. Error: {}", config.getMaxRetries(), ex.getMessage());
            return retry(function);
        }
    }


    private T retry(Supplier<T> function)
    {
        int retryCounter = 0;
        while (retryCounter < config.getMaxRetries())
        {
            try
            {
                log.info("Retrying command. Backing off for {} seconds", config.getBackoffSeconds());
                Thread.sleep(Duration.ofSeconds(config.getBackoffSeconds()).toMillis());
                return function.get();
            }
            catch (Exception ex)
            {
                retryCounter++;
                log.warn("FAILED - Command failed on retry {} of {}. Error: {}", retryCounter, config.getMaxRetries(), ex.getMessage());
                if (retryCounter >= config.getMaxRetries())
                {
                    log.warn("FAILED - Max retries exceeded.");
                    break;
                }
            }
        }
        log.warn("Command failed on all of {} retries", config.getMaxRetries());
        return null;
    }
}
