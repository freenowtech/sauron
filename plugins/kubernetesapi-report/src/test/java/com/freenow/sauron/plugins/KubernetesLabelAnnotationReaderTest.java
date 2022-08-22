package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.readers.KubernetesLabelAnnotationReader;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.freenow.sauron.plugins.utils.KubernetesResources.CRONJOB;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesLabelAnnotationReaderTest
{
    private static final String SERVICE_LABEL = "label/service.name";
    private static final String SERVICE_NAME = "serviceName";
    private static final String TEST_ANNOTATION = "test/annotation";
    private static final String TEST_LABEL = "test/label";

    @Mock
    private ApiClient apiClient;

    @Mock
    private KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand;

    @InjectMocks
    private KubernetesLabelAnnotationReader kubernetesLabelAnnotationReader;


    @Test
    public void read()
    {
        final var objMetaData = createObjMetaData();
        final var input = dummyDataSet();
        when(kubernetesGetObjectMetaCommand.get(SERVICE_LABEL, CRONJOB, SERVICE_NAME, apiClient)).thenReturn(objMetaData);

        kubernetesLabelAnnotationReader.read(input, SERVICE_LABEL, dummySelectors(), apiClient);
        assertNotNull(input);
        assertEquals(SERVICE_NAME, input.getStringAdditionalInformation(TEST_LABEL).get());
        assertEquals(SERVICE_NAME, input.getStringAdditionalInformation(TEST_ANNOTATION).get());
    }


    @Test
    public void readDontModifyInputWhitNoSelectors()
    {
        final var input = dummyDataSet();
        kubernetesLabelAnnotationReader.read(input, SERVICE_LABEL, emptyMap(), apiClient);
        assertNotNull(input);
        assertEquals(input, dummyDataSet());
        verifyZeroInteractions(kubernetesGetObjectMetaCommand);
    }


    @Test
    public void readDontModifyInputWhenException()
    {
        final var input = dummyDataSet();
        doThrow(new RuntimeException()).when(kubernetesGetObjectMetaCommand).get(SERVICE_LABEL, CRONJOB, SERVICE_NAME, apiClient);

        kubernetesLabelAnnotationReader.read(input, SERVICE_LABEL, dummySelectors(), apiClient);
        assertNotNull(input);
        assertEquals(input, dummyDataSet());
    }


    private static Map<String, Map<?, ?>> dummySelectors()
    {
        Map<String, Map<?, ?>> selectors = new HashMap<>();
        selectors.put("deployment", Map.of("0", TEST_ANNOTATION, "1", TEST_LABEL));
        selectors.put("cronjob", Map.of("0", TEST_ANNOTATION, "1", TEST_LABEL));
        return selectors;
    }


    private DataSet dummyDataSet()
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName(SERVICE_NAME);
        return dataSet;
    }


    private Optional<V1ObjectMeta> createObjMetaData()
    {
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setLabels(Map.of(SERVICE_LABEL, SERVICE_NAME));
        objectMeta.setName(SERVICE_NAME);
        objectMeta.setAnnotations(Map.of(TEST_ANNOTATION, SERVICE_NAME));
        objectMeta.setLabels(Map.of(TEST_LABEL, SERVICE_NAME));
        return Optional.of(objectMeta);
    }
}
