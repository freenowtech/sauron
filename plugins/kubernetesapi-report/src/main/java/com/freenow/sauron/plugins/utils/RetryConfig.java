package com.freenow.sauron.plugins.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RetryConfig
{
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_BACKOFF_SECONDS = 30;

    private final int maxRetries;
    private final int backoffSeconds;


    public RetryConfig()
    {
        this.maxRetries = DEFAULT_MAX_RETRIES;
        this.backoffSeconds = DEFAULT_BACKOFF_SECONDS;
    }
}