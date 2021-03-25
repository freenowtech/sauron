package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.jgit.api.CloneCommand;

public final class CloneCommandFactory
{
    static final String SCHEMA_SSH = "ssh";
    static final String SCHEMA_HTTPS = "https";
    static final String SCHEMA_HTTP = "http";


    public static CloneCommand instance(String repositoryUrl, Path destination, PluginsConfigurationProperties properties)
    {
        URI repositoryUri = URI.create(repositoryUrl);
        switch (repositoryUri.getScheme())
        {
            case SCHEMA_SSH:
                return new SshCloneCommand(repositoryUrl, destination, properties);
            case SCHEMA_HTTP:
            case SCHEMA_HTTPS:
            default:
                return new HttpsCloneCommand(repositoryUrl, destination, properties);
        }
    }


    private CloneCommandFactory()
    {
        throw new UnsupportedOperationException();
    }
}