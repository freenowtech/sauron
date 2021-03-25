package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.nio.file.Path;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import static com.freenow.sauron.plugins.utils.CloneCommandHelper.getStringProperty;

public final class HttpsCloneCommand extends CloneCommand
{
    HttpsCloneCommand(String repositoryUrl, Path destination, PluginsConfigurationProperties properties)
    {
        this.setURI(repositoryUrl).setDirectory(destination.toFile());
        setCredentials(properties);
    }


    private void setCredentials(PluginsConfigurationProperties properties)
    {
        getStringProperty("user", properties).ifPresent(user ->
            getStringProperty("password", properties).ifPresent(password ->
                this.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password))
            ));
    }
}