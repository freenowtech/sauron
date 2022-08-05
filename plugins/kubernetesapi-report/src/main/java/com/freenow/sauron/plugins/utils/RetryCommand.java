package com.freenow.sauron.plugins.utils;

import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RetryCommand<T>
{
    private static final int DEFAULT_MAX_RETRIES = 3;

    private static final int DEFAULT_BACKOFF_SECONDS = 30;

    private final int maxRetries;

    private final int backoffSeconds;


    public RetryCommand()
    {
        maxRetries = DEFAULT_MAX_RETRIES;
        backoffSeconds = DEFAULT_BACKOFF_SECONDS;
    }


    // Takes a function and executes it, if fails, passes the function to the retry command
    public T run(Supplier<T> function)
    {
        try
        {
            return function.get();
        }
        catch (Exception ex)
        {
            log.warn("FAILED - Command failed, will be retried {} times. Error: {}", maxRetries, ex.getMessage());
            return retry(function);
        }
    }


    private T retry(Supplier<T> function)
    {
        int retryCounter = 0;
        while (retryCounter < maxRetries)
        {
            try
            {
                log.info("Retrying command. Backing off for {} seconds", backoffSeconds);
                Thread.sleep(Duration.ofSeconds(backoffSeconds).toMillis());
                return function.get();
            }
            catch (Exception ex)
            {
                retryCounter++;
                log.warn("FAILED - Command failed on retry {} of {}. Error: {}", retryCounter, maxRetries, ex.getMessage());
                if (retryCounter >= maxRetries)
                {
                    log.warn("FAILED - Max retries exceeded.");
                    break;
                }
            }
        }
        log.warn("Command failed on all of {} retries", maxRetries);
        return null;
    }
}
