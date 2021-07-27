package com.freenow.sauron.service;

import com.freenow.sauron.plugins.AutomaticPluginUpdater;
import com.freenow.sauron.plugins.artifactory.ArtifactoryDownloader;
import com.freenow.sauron.plugins.artifactory.ArtifactoryRepository;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.assertj.core.util.Strings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pf4j.AbstractPluginManager;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.update.FileVerifier;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateManager;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AutomaticPluginUpdaterTest
{
    private static final String PLUGIN_ID = "console-output";

    private static final String PLUGIN_JAR = Strings.concat(PLUGIN_ID, ".jar");

    private static Path pluginSource;

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public TestName name = new TestName();

    @Mock
    private FileVerifier verifier;

    @Mock
    private ArtifactoryDownloader downloader;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ArtifactoryRepository repository;

    private UpdateManager updateManager;

    private AutomaticPluginUpdater automaticPluginUpdater;

    private Path tempPluginPath;


    @BeforeClass
    public static void init() throws IOException, URISyntaxException
    {
        Path tempPath = temporaryFolder.newFolder("plugins").toPath();
        System.setProperty(AbstractPluginManager.PLUGINS_DIR_PROPERTY_NAME, tempPath.toString());
        pluginSource = Paths.get(Objects.requireNonNull(AutomaticPluginUpdaterTest.class.getClassLoader().getResource(PLUGIN_JAR)).toURI());
    }


    @Before
    public void initEach() throws IOException
    {
        String pluginsTestPath = Strings.concat("plugins/", name.getMethodName());
        Path tempPath = temporaryFolder.newFolder(pluginsTestPath).toPath();
        tempPluginPath = tempPath.resolve(PLUGIN_JAR);
        when(downloader.downloadFile(any())).thenReturn(tempPluginPath);

        repository = spy(new ArtifactoryRepository(null, downloader, null));
        when(repository.getFileVerifier()).thenReturn(verifier);
        doNothing().when(repository).refresh();

        PluginManager pluginManager = new DefaultPluginManager();
        updateManager = spy(new UpdateManager(pluginManager, Collections.singletonList(repository)));
        automaticPluginUpdater = new AutomaticPluginUpdater(updateManager, pluginManager, applicationEventPublisher);
    }


    @Test
    public void testPluginInstallation() throws IOException
    {
        installVersion("0.0.7");
        verify(updateManager).installPlugin(PLUGIN_ID, "0.0.7");
    }


    @Test
    public void testPluginUpdate() throws IOException
    {
        installVersion("0.0.7");
        verify(updateManager).installPlugin(PLUGIN_ID, "0.0.7");

        installVersion("0.0.7", "0.0.8");
        verify(updateManager).updatePlugin(PLUGIN_ID, "0.0.8");
    }


    @Test
    public void testPluginUpdateStringLexicalComparison() throws IOException
    {
        installVersion("0.0.7");
        verify(updateManager).installPlugin(PLUGIN_ID, "0.0.7");

        installVersion("0.0.7", "0.0.10");
        verify(updateManager).updatePlugin(PLUGIN_ID, "0.0.10");
    }


    private Map<String, PluginInfo> getPlugins(String... versions)
    {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.id = PLUGIN_ID;
        pluginInfo.releases = getPluginRelease(versions);

        Map<String, PluginInfo> plugins = new HashMap<>();
        plugins.put(PLUGIN_ID, pluginInfo);
        return plugins;
    }


    private List<PluginInfo.PluginRelease> getPluginRelease(String... versions)
    {
        return Arrays.stream(versions).map(version ->
        {
            PluginInfo.PluginRelease pluginRelease = new PluginInfo.PluginRelease();
            pluginRelease.version = version;
            pluginRelease.url = "http://localhost.com";
            return pluginRelease;
        }).collect(Collectors.toList());
    }

    private void installVersion(String... versions) throws IOException
    {
        Files.copy(pluginSource, tempPluginPath, StandardCopyOption.REPLACE_EXISTING);
        when(repository.getPlugins()).thenReturn(getPlugins(versions));
        automaticPluginUpdater.update();
    }
}