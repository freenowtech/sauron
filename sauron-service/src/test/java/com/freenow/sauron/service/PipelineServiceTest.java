package com.freenow.sauron.service;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.SauronExtension;
import com.freenow.sauron.properties.PipelineConfigurationProperties;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.pf4j.PluginManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PipelineServiceTest extends UtilsBaseTest
{

    private static final String ELASTICSEARCH_OUTPUT_PLUGIN = "elasticsearch-output";

    private static final String REPROCESS_PLUGIN = "plugin-123";

    @Mock
    private PluginManager pluginManager;

    @Mock
    private SauronExtension extension;

    @Mock
    private PipelineConfigurationProperties pipelineProperties;

    @Mock
    private PluginsConfigurationProperties pluginsProperties;

    @Spy
    private RequestHandler requestHandler;

    @Spy
    @InjectMocks
    private PipelineService pipelineService;


    @Test
    public void testProcessEmptyPipeline()
    {
        doReturn(Collections.emptyList()).when(pipelineProperties).getDefaultPipeline();
        pipelineService.process(new BuildRequest());
        verify(pluginManager, never()).getExtensions(eq(SauronExtension.class), anyString());
    }


    @Test
    public void testProcessDefaultPipeline()
    {
        doReturn(Collections.singletonList("plugin")).when(pipelineProperties).getDefaultPipeline();
        pipelineService.process(new BuildRequest());
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), anyString());
    }


    @Test
    public void testProcessDefaultPipelineExistingPlugin()
    {
        doReturn(Collections.singletonList("plugin")).when(pipelineProperties).getDefaultPipeline();
        doReturn(Collections.singletonList(extension)).when(pluginManager).getExtensions(eq(SauronExtension.class), eq("plugin"));
        pipelineService.process(new BuildRequest());
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), anyString());
        verify(extension, times(1)).apply(eq(pluginsProperties), any(DataSet.class));
    }


    @Test
    public void testProcessDefaultPipelineNotExistingPlugin()
    {
        doReturn(Collections.singletonList("invalidPlugin")).when(pipelineProperties).getDefaultPipeline();
        doReturn(Collections.singletonList(extension)).when(pluginManager).getExtensions(eq(SauronExtension.class), eq("plugin"));
        pipelineService.process(new BuildRequest());
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), anyString());
        verify(extension, never()).apply(any(PluginsConfigurationProperties.class), any(DataSet.class));
    }


    @Test
    public void testProcessExceptionThrown()
    {
        doReturn(Collections.singletonList("plugin")).when(pipelineProperties).getDefaultPipeline();
        doThrow(RuntimeException.class).when(pluginManager).getExtensions(any(), anyString());
        pipelineService.process(new BuildRequest());
        verify(pluginManager, times(1)).getExtensions(any(), anyString());
        verify(extension, never()).apply(any(PluginsConfigurationProperties.class), any(DataSet.class));
    }


    @Test
    public void pluginShouldRunWhenDefaultPipelineIsEmpty()
    {
        doReturn(Collections.emptyList()).when(pipelineProperties).getDefaultPipeline();

        pipelineService.process(buildReprocessRequest());

        verify(pipelineService).runPlugin(eq(REPROCESS_PLUGIN), any(), any());
        verify(pipelineService).runPlugin(eq(ELASTICSEARCH_OUTPUT_PLUGIN), any(), any());
    }


    @Test
    public void pluginShouldRunWhenNotPresentInDefaultPipeline()
    {
        doReturn(Collections.singletonList("random-plugin")).when(pipelineProperties).getDefaultPipeline();

        pipelineService.process(buildReprocessRequest());

        verify(pipelineService, times(0)).runPlugin(eq("random-plugin"), any(), any());
        verify(pipelineService).runPlugin(eq(REPROCESS_PLUGIN), any(), any());
        verify(pipelineService).runPlugin(eq(ELASTICSEARCH_OUTPUT_PLUGIN), any(), any());
    }


    @Test
    public void pluginShouldRunOnlyOnceWhenPresentInDefaultPipeline()
    {
        doReturn(Collections.singletonList(REPROCESS_PLUGIN)).when(pipelineProperties).getDefaultPipeline();

        pipelineService.process(buildReprocessRequest());

        verify(pipelineService, atMost(1)).runPlugin(eq(REPROCESS_PLUGIN), any(), any());
        verify(pipelineService).runPlugin(eq(ELASTICSEARCH_OUTPUT_PLUGIN), any(), any());
    }


    @Test
    public void pluginDependenciesShouldRun()
    {
        doReturn(Arrays.asList("dependency-1", "dependency-2", REPROCESS_PLUGIN, "another-plugin")).when(pipelineProperties).getDefaultPipeline();

        pipelineService.process(buildReprocessRequest());

        verify(pipelineService, times(0)).runPlugin(eq("another-plugin"), any(), any());
        verify(pipelineService).runPlugin(eq("dependency-1"), any(), any());
        verify(pipelineService).runPlugin(eq("dependency-2"), any(), any());
        verify(pipelineService, atMost(1)).runPlugin(eq(REPROCESS_PLUGIN), any(), any());
        verify(pipelineService).runPlugin(eq(ELASTICSEARCH_OUTPUT_PLUGIN), any(), any());
    }


    private BuildRequest buildReprocessRequest()
    {
        final BuildRequest request = new BuildRequest();
        request.setPlugin(REPROCESS_PLUGIN);

        return request;
    }
}