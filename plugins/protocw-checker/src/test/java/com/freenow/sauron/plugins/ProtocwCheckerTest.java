package com.freenow.sauron.plugins;

import arrow.core.Either;
import com.freenow.sauron.plugins.protocw.Checker;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.freenow.sauron.plugins.ProtocwChecker.INPUT_REPO_PATH;
import static com.freenow.sauron.plugins.ProtocwChecker.OUTPUT_MISSING_PROTOCW;
import static com.freenow.sauron.plugins.ProtocwChecker.OUTPUT_PROTOC_VERSION;
import static com.freenow.sauron.plugins.ProtocwChecker.PROP_PROTOCW_PROPERTIRES_FILE_NAME;
import static com.freenow.sauron.plugins.ProtocwChecker.PROP_PROTOC_FILE_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProtocwCheckerTest
{
    private ProtocwChecker subject;
    private Checker mockChecker;


    @Before
    public void setUp()
    {
        mockChecker = Mockito.mock(Checker.class);
        subject = new ProtocwChecker(mockChecker);
    }


    @Test
    public void test_when_the_sauron_calls_the_checker_then_the_props_and_intput_get_passed_to_the_checker()
    {
        //Given: a dataset
        DataSet dataSet = new DataSet();
        dataSet.setAdditionalInformation(INPUT_REPO_PATH, "/some/repo/");
        //And: Some properties
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        HashMap<String, Object> propMap = new HashMap<>();
        propMap.put(PROP_PROTOC_FILE_NAME, "protocw");
        propMap.put(PROP_PROTOCW_PROPERTIRES_FILE_NAME, "protocw.properties");
        properties.put("protocw-checker", propMap);
        //And: a checker that succedes
        when(mockChecker.apply(any(), any(), any())).thenReturn(new Either.Right<String>("2.5.0"));

        //When: the sauron checker get called
        subject.apply(properties, dataSet);

        // Then: the protocw checker gets called
        ArgumentCaptor<Optional> repoPath = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<Optional> protocwFileName = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<Optional> protocwPropertiesFileName = ArgumentCaptor.forClass(Optional.class);
        verify(mockChecker, times(1)).apply(
            repoPath.capture(), protocwFileName.capture(), protocwPropertiesFileName.capture()
        );
        //And: with the correct parameters
        assertEquals(Optional.of("/some/repo/"), repoPath.getValue());
        assertEquals(Optional.of("protocw"), protocwFileName.getValue());
        assertEquals(Optional.of("protocw.properties"), protocwPropertiesFileName.getValue());
    }


    @Test
    public void test_when_the_protocw_cheker_returns_left_then_no_data_gets_added()
    {
        //Given: a checker that fails
        when(mockChecker.apply(any(), any(), any())).thenReturn(new Either.Left<String>("Error!!"));
        //And: a dataset
        DataSet dataSet = new DataSet();
        //And: properties
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();

        //When: the sauron checker get called
        DataSet output = subject.apply(properties, dataSet);

        //Then: no data should be added
        assertEquals("The protoc version additional data should be empty",
                     Optional.empty(), output.getObjectAdditionalInformation(OUTPUT_PROTOC_VERSION));
        assertEquals("The protocw missing additional data should be empty",
                     Optional.empty(), output.getObjectAdditionalInformation(OUTPUT_MISSING_PROTOCW));
    }


    @Test
    public void test_when_the_protocw_cheker_returns_a_semver_then_that_gets_added_as_additional_data()
    {
        //Given: a checker that succedes
        when(mockChecker.apply(any(), any(), any())).thenReturn(new Either.Right<String>("2.5.0"));
        //And: a dataset
        DataSet dataSet = new DataSet();
        //And: properties
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();

        //When: the sauron checker get called
        DataSet output = subject.apply(properties, dataSet);

        //Then: no data should be added
        assertEquals("The protoc version additional data should be empty",
                     Optional.of("2.5.0"), output.getObjectAdditionalInformation(OUTPUT_PROTOC_VERSION));
        assertEquals("The protocw missing additional data should be empty",
                     Optional.of(false), output.getObjectAdditionalInformation(OUTPUT_MISSING_PROTOCW));
    }


    @Test
    public void test_when_the_protocw_cheker_returns_a_empty_semver_then_that_gets_added_as_additional_data()
    {
        //Given: a checker that succedes
        when(mockChecker.apply(any(), any(), any())).thenReturn(new Either.Right<String>(null));
        //And: a dataset
        DataSet dataSet = new DataSet();
        //And: properties
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();

        //When: the sauron checker get called
        DataSet output = subject.apply(properties, dataSet);

        //Then: no data should be added
        assertEquals("The protoc version additional data should be empty",
                     Optional.empty(), output.getObjectAdditionalInformation(OUTPUT_PROTOC_VERSION));
        assertEquals("The protocw missing additional data should be empty",
                     Optional.of(true), output.getObjectAdditionalInformation(OUTPUT_MISSING_PROTOCW));
    }
}