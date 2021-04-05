package com.freenow.sauron.plugins.artifactory;

import com.freenow.sauron.properties.ArtifactoryConfigurationProperties;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.jfrog.artifactory.client.model.RepoPath;
import org.pf4j.update.FileDownloader;
import org.pf4j.update.FileVerifier;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateRepository;
import org.pf4j.update.verifier.BasicVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@EnableConfigurationProperties(ArtifactoryConfigurationProperties.class)
public class ArtifactoryRepository implements UpdateRepository
{
    private final ArtifactoryConfigurationProperties properties;

    private final Artifactory artifactory;

    private final ArtifactoryDownloader artifactoryDownloader;

    private Map<String, PluginInfo> plugins;


    public ArtifactoryRepository(Artifactory artifactory, ArtifactoryDownloader artifactoryDownloader, ArtifactoryConfigurationProperties properties)
    {
        this.artifactory = artifactory;
        this.artifactoryDownloader = artifactoryDownloader;
        this.properties = properties;
    }


    @Override
    public String getId()
    {
        return ArtifactoryRepository.class.getName();
    }


    @Override
    public URL getUrl()
    {
        return null;
    }


    @Override
    public Map<String, PluginInfo> getPlugins()
    {
        if (plugins == null)
        {
            refresh();
        }

        return plugins;
    }


    @Override
    public PluginInfo getPlugin(String id)
    {
        return getPlugins().getOrDefault(id, null);
    }


    @Override
    public void refresh()
    {
        try
        {
            plugins = new HashMap<>();
            Folder folder = artifactory.repository(properties.getRepository()).folder(properties.getGroupId()).info();

            for (Item plugin : folder.getChildren())
            {
                Folder releases = artifactory.repository(properties.getRepository()).folder(folder.getPath() + "/" + plugin.getName()).info();
                if (releases.isFolder() && !releases.getChildren().isEmpty())
                {
                    PluginInfo pluginInfo = new PluginInfo();
                    pluginInfo.id = plugin.getName();
                    pluginInfo.name = plugin.getName();
                    pluginInfo.releases = new ArrayList<>();

                    for (Item release : releases.getChildren().stream().filter(Item::isFolder).collect(Collectors.toList()))
                    {
                        PluginInfo.PluginRelease pluginRelease = new PluginInfo.PluginRelease();
                        pluginRelease.version = release.getName();
                        pluginRelease.date = new Date();
                        pluginRelease.url = String.format("%s/%s/%s", artifactory.getUri(), properties.getRepository(), getReleaseUrl(pluginInfo.name, release.getName()));

                        pluginInfo.releases.add(pluginRelease);
                    }

                    plugins.put(pluginInfo.id, pluginInfo);
                }
            }
        }
        catch (MalformedURLException ex)
        {
            log.error(ex.getMessage(), ex);
        }
    }


    @Override
    public FileDownloader getFileDownloader()
    {
        return artifactoryDownloader;
    }


    @Override
    public FileVerifier getFileVerifier()
    {
        return new BasicVerifier();
    }


    private String getReleaseUrl(String pluginName, String pluginVersion)
    {
        List<RepoPath> results = artifactory.searches().artifactsByGavc()
            .repositories(properties.getRepository())
            .groupId(properties.getGroupId())
            .artifactId(pluginName)
            .version(pluginVersion)
            .doSearch();

        return results.stream()
            .filter(item -> item.getItemPath().matches(".*.jar"))
            .max(Comparator.comparing(RepoPath::getItemPath))
            .map(RepoPath::getItemPath)
            .orElse(null);
    }
}