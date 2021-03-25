package com.freenow.sauron.plugins.protocw;

import com.freenow.sauron.plugins.protocw.DefaultService;
import com.freenow.sauron.plugins.protocw.Service;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultServiceTest
{
    private Service subject;


    @Before
    public void setUp()
    {
        subject = new DefaultService();
    }


    @Test
    public void test_when_there_is_no_pom_the_service_reports_missing_protocw()
    {
        // When: checking for a non existant protocw file
        String response = subject.check(Paths.get("/does/not/exist/protocw"), Paths.get("/does/not/exist/protocw.properties"));

        // Then: the check fails
        assertNull("the report returns empty", response);
    }


    @Test
    public void test_when_the_protocw_file_does_not_exist_then_the_service_reports_missing_protocw()
    {
        // When: checking for a non existant protocw file
        String response = subject.check(Paths.get("/does/not/exist/protocw"), Paths.get("/does/not/exist/protocw.properties"));

        // Then: the check fails
        assertNull("the report returns empty", response);
    }


    @Test
    public void test_when_the_protocw_file_exists_but_the_properties_doesent_then_the_service_reports_missing_protocw() throws IOException
    {
        //Given: a protocw file
        Path protocw = createFile("protocw", "some bash file");

        // When: checking for a non existant protocw properties file
        String response = subject.check(protocw, Paths.get("/does/not/exist/protocw.properties"));

        // Then: the check fails
        assertNull("the report returns empty", response);
    }


    @Test
    public void test_when_both_protocw_and_properties_then_the_service_reports_the_protocw_and_version() throws IOException
    {
        //Given: a protocw file
        Path protocw = createFile("protocw", "some bash file");
        //And: a protow.properties
        Path protocwProperties = createFile("protocw.properties", "protoc_version=2.5.0");

        // When: checking for a non existant protocw properties file
        String response = subject.check(protocw, protocwProperties);

        // Then: the check fails
        assertEquals("2.5.0", response);
    }


    @Test
    public void test_when_the_properties_is_malformed_then_the_service_reports_the_protocw_but_no_version() throws IOException
    {
        //Given: a protocw file
        Path protocw = createFile("protocw", "some bash file");
        //And: a protow.properties
        Path protocwProperties = createFile("protocw.properties", "foo");

        // When: checking for a non existant protocw properties file
        String response = subject.check(protocw, protocwProperties);

        // Then: the check fails
        assertEquals("malformed_version", response);
    }


    private Path createFile(String name, String content) throws IOException
    {
        Path file = Files.createTempFile(name, null);
        FileOutputStream stream = new FileOutputStream(file.toFile());
        stream.write(content.getBytes());
        return file;
    }


}