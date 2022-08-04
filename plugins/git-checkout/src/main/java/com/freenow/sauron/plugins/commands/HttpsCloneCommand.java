package com.freenow.sauron.plugins.commands;

import java.nio.file.Path;
import java.util.Map;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import static com.freenow.sauron.plugins.Constants.EMPTY_STRING;
import static com.freenow.sauron.plugins.Constants.PASSWORD;
import static com.freenow.sauron.plugins.Constants.USER;

public final class HttpsCloneCommand extends CloneCommand
{
    HttpsCloneCommand(String repositoryUrl, Path destination, Map<String, Object> properties)
    {
        this.setURI(repositoryUrl).setDirectory(destination.toFile());
        if (properties.containsKey(USER))
        {
            String user = String.valueOf(properties.get(USER));
            String password = String.valueOf(properties.getOrDefault(PASSWORD, EMPTY_STRING));
            this.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password));
        }
    }
}