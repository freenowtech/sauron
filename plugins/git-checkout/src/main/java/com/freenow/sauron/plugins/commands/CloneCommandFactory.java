package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.CloneCommand;

import static com.freenow.sauron.plugins.Constants.HOST;
import static com.freenow.sauron.plugins.Constants.PLUGIN_ID;
import static com.freenow.sauron.plugins.Constants.REPOSITORIES;
import static com.freenow.sauron.plugins.Constants.SCHEMA_HTTP;
import static com.freenow.sauron.plugins.Constants.SCHEMA_HTTPS;
import static com.freenow.sauron.plugins.Constants.SCHEMA_SSH;

public final class CloneCommandFactory
{
    public static CloneCommand instance(String repositoryUrl, Path destination, PluginsConfigurationProperties properties)
    {
        URI repositoryUri = URI.create(repositoryUrl);
        Map<String, Object> repoProperties = getRepositoryProperty(repositoryUri.getHost(), properties);
        switch (repositoryUri.getScheme())
        {
            case SCHEMA_SSH:
                return new SshCloneCommand(repositoryUrl, destination, repoProperties);
            case SCHEMA_HTTP:
            case SCHEMA_HTTPS:
            default:
                return new HttpsCloneCommand(repositoryUrl, destination, repoProperties);
        }
    }


    private CloneCommandFactory()
    {
        throw new UnsupportedOperationException();
    }


    private static Map<String, Object> getRepositoryProperty(String host, PluginsConfigurationProperties properties)
    {
        LinkedHashMap repositoriesConfig = properties.getPluginConfigurationProperty(PLUGIN_ID, REPOSITORIES)
            .filter(LinkedHashMap.class::isInstance)
            .map(LinkedHashMap.class::cast)
            .orElse(new LinkedHashMap());

        for (Object repositoryConfig : repositoriesConfig.values())
        {
            Map repoMap = repositoryConfig instanceof Map ? (Map) repositoryConfig : Collections.emptyMap();
            String hostPattern = (String) repoMap.getOrDefault(HOST, "");
            Pattern pattern = Pattern.compile(hostPattern);
            if (pattern.matcher(host).find())
            {
                return repoMap;
            }
        }
        return Collections.emptyMap();
    }
}