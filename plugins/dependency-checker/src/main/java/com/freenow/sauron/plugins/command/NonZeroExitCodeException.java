package com.freenow.sauron.plugins.command;

public class NonZeroExitCodeException extends RuntimeException
{
    NonZeroExitCodeException(String command, String details)
    {
        super("`" + command + "` did finish with non-zero exit code. " + details);
    }
}
