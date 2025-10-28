package com.freenow.sauron.service;

import com.freenow.sauron.datatransferobject.BuildRequest;
import com.freenow.sauron.handler.RequestHandler;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.SauronExtension;
import com.freenow.sauron.properties.PipelineConfigurationProperties;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.pf4j.PluginManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;


@RunWith(MockitoJUnitRunner.class)
public class PipelineServiceTest
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

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Timer.Builder timerBuilder;

    @Mock
    private Timer timer;

    private PipelineService pipelineService;


    @Before
    public void setup()
    {
        // Initialize the spy here
        pipelineService = spy(new PipelineService(
            pluginManager,
            pipelineProperties,
            pluginsProperties,
            requestHandler,
            meterRegistry
        ));

        when(pipelineProperties.getMandatoryOutputPlugin()).thenReturn(ELASTICSEARCH_OUTPUT_PLUGIN);

        when(pluginManager.getExtensions(eq(SauronExtension.class), anyString())).thenReturn(Collections.singletonList(extension));

        when(meterRegistry.counter(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(counter);

        doReturn(timerBuilder).when(pipelineService).getTimerBuilder(anyString());
        when(timerBuilder.tag(anyString(), anyString())).thenReturn(timerBuilder);
        when(timerBuilder.register(any(MeterRegistry.class))).thenReturn(timer);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
            .when(timer).record(any(Supplier.class));
    }

    @Test
    public void testProcessEmptyPipeline()
    {
        doReturn(Collections.emptyList()).when(pipelineProperties).getDefaultPipeline();
        pipelineService.process(buildDefaultRequest());
        verify(pluginManager, never()).getExtensions(eq(SauronExtension.class), anyString());
    }


    @Test
    public void testProcessDefaultPipelineExistingPlugin()
    {
        doReturn(Collections.singletonList("plugin")).when(pipelineProperties).getDefaultPipeline();
        when(extension.apply(any(PluginsConfigurationProperties.class), any(DataSet.class))).thenAnswer(invocation -> invocation.getArgument(1));
        pipelineService.process(buildDefaultRequest());
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq("plugin"));
        verify(extension, times(1)).apply(any(PluginsConfigurationProperties.class), any(DataSet.class));
    }


    @Test
    public void testProcessDefaultPipelineNotExistingPlugin()
    {
        doReturn(Collections.singletonList("invalidPlugin")).when(pipelineProperties).getDefaultPipeline();
        when(pluginManager.getExtensions(eq(SauronExtension.class), eq("invalidPlugin"))).thenReturn(Collections.emptyList());
        pipelineService.process(buildDefaultRequest());
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), anyString());
        verify(extension, never()).apply(any(PluginsConfigurationProperties.class), any(DataSet.class));
    }


    @Test
    public void testProcessExceptionThrown()
    {
        doReturn(Collections.singletonList("plugin")).when(pipelineProperties).getDefaultPipeline();
        doThrow(RuntimeException.class).when(pluginManager).getExtensions(any(), eq("plugin"));
        pipelineService.process(buildDefaultRequest());
        verify(pluginManager, times(1)).getExtensions(any(), anyString());
        verify(extension, never()).apply(any(PluginsConfigurationProperties.class), any(DataSet.class));
    }


    @Test
    public void pluginShouldRunWhenDefaultPipelineIsEmpty()
    {
        doReturn(Collections.emptyList()).when(pipelineProperties).getDefaultPipeline();
        when(extension.apply(any(PluginsConfigurationProperties.class), any(DataSet.class))).thenAnswer(invocation -> invocation.getArgument(1));

        pipelineService.process(buildReprocessRequest());

        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq(REPROCESS_PLUGIN));
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq(ELASTICSEARCH_OUTPUT_PLUGIN));
    }


    @Test
    public void pluginShouldRunWhenNotPresentInDefaultPipeline()
    {
        doReturn(Collections.singletonList("random-plugin")).when(pipelineProperties).getDefaultPipeline();
        when(extension.apply(any(PluginsConfigurationProperties.class), any(DataSet.class))).thenAnswer(invocation -> invocation.getArgument(1));

        pipelineService.process(buildReprocessRequest());

        verify(pluginManager, never()).getExtensions(eq(SauronExtension.class), eq("random-plugin"));
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq(REPROCESS_PLUGIN));
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq(ELASTICSEARCH_OUTPUT_PLUGIN));
    }


    @Test
    public void pluginShouldRunOnlyOnceWhenPresentInDefaultPipeline()
    {
        doReturn(Collections.singletonList(REPROCESS_PLUGIN)).when(pipelineProperties).getDefaultPipeline();
        when(extension.apply(any(PluginsConfigurationProperties.class), any(DataSet.class))).thenAnswer(invocation -> invocation.getArgument(1));

        pipelineService.process(buildReprocessRequest());

        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq(REPROCESS_PLUGIN));
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq(ELASTICSEARCH_OUTPUT_PLUGIN));
    }


    @Test
    public void pluginDependenciesShouldRun()
    {
        doReturn(Arrays.asList("dependency-1", "dependency-2", REPROCESS_PLUGIN, "another-plugin")).when(pipelineProperties).getDefaultPipeline();
        when(extension.apply(any(PluginsConfigurationProperties.class), any(DataSet.class))).thenAnswer(invocation -> invocation.getArgument(1));

        pipelineService.process(buildReprocessRequest());

        verify(pluginManager, never()).getExtensions(eq(SauronExtension.class), eq("another-plugin"));
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq("dependency-1"));
        verify(pluginManager, times(1)).getExtensions(eq(SauronExtension.class), eq("dependency-2"));
        verify(pluginManager, atLeastOnce()).getExtensions(eq(SauronExtension.class), eq(REPROCESS_PLUGIN));
    }

    private BuildRequest buildDefaultRequest()
    {
        final BuildRequest request = new BuildRequest();
        request.setServiceName("test-service");
        request.setCommitId("abc1234");
        request.setBuildId(UUID.randomUUID().toString());
        return request;
    }


    private BuildRequest buildReprocessRequest()
    {
        final BuildRequest request = buildDefaultRequest();
        request.setPlugin(REPROCESS_PLUGIN);

        return request;
    }
}
