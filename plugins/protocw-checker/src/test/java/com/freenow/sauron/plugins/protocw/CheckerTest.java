package com.freenow.sauron.plugins.protocw;

import arrow.core.Either;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import kotlin.Unit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckerTest
{
    private Service mockService;
    private Validator mockValidator;
    private Checker subject;
    private Path repository;


    @Before
    public void setUp()
    {
        mockService = Mockito.mock(Service.class);
        mockValidator = Mockito.mock(Validator.class);
        subject = new DefaultChecker(mockService, mockValidator);
        repository = Paths.get("/foo/bar");
    }


    @Test
    public void test_when_the_repositoy_path_is_not_set_then_it_fails()
    {
        //Given: no repository
        Optional<String> emptyRepository = Optional.empty();

        // When: checking
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        Either<String, String> output = subject.apply(emptyRepository, Optional.empty(), Optional.empty());

        // Then: it fails because repositoryPath was not set
        assertEquals("should fail",
                     new Either.Left<String>("repositoryPath was not set"), output);
    }


    @Test
    public void test_when_the_repository_does_not_need_protoc_then_no_additional_properties_get_added()
    {
        //Given: a repo that does not needs protoc
        when(mockValidator.check(any())).thenReturn(new Either.Left<String>("foo"));

        // When: checking
        Either<String, String> output = subject.apply(Optional.of("no-repo-path"), Optional.empty(), Optional.empty());

        // Then: it fails with the validator error
        assertEquals("should fail",
                     new Either.Left<String>("Protocw not needed: foo"), output);
    }


    @Test
    public void test_when_no_properties_are_given_the_default_ones_are_set()
    {
        //Given: a repo that need protoc
        when(mockValidator.check(any())).thenReturn(new Either.Right<Unit>(Unit.INSTANCE));

        // When: gets checked with no properties
        Either<String, String> output = subject.apply(Optional.of("/some/repo/"),
                                                      Optional.empty(), Optional.empty());

        // Then: the service gets called with the default values
        ArgumentCaptor<Path> protocwFileName = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<Path> protocwPropertiesFileName = ArgumentCaptor.forClass(Path.class);

        verify(mockService, times(1)).check(protocwFileName.capture(), protocwPropertiesFileName.capture());
        assertEquals("protocw", protocwFileName.getValue().getFileName().toString());
        assertEquals("protocw.properties", protocwPropertiesFileName.getValue().getFileName().toString());
    }


}