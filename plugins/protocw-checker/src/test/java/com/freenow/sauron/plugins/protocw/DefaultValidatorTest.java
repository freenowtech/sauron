package com.freenow.sauron.plugins.protocw;

import arrow.core.Either;
import com.freenow.sauron.plugins.protocw.DefaultValidator;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import kotlin.Unit;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultValidatorTest
{
    private static final String EMPTY_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
        "</project>";
    private static final String PROTOC_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
        "    <build>\n" +
        "        <plugins>\n" +
        "            <plugin>\n" +
        "                <groupId>com.github.igor-petruk.protobuf</groupId>\n" +
        "                <artifactId>protobuf-maven-plugin</artifactId>\n" +
        "                <version>${protobuf-maven-plugin.version}</version>\n" +
        "            </plugin>" +
        "        </plugins>\n" +
        "    </build>\n" +
        "</project>\n";

    private DefaultValidator subject;


    @Before
    public void setUp()
    {
        subject = new DefaultValidator();
    }


    @Test
    public void test_when_there_is_no_repository_then_the_validation_fails()
    {
        // When: validating for a non existant repository
        Either<String, Unit> response = subject.check(Paths.get("/does/not/exist/"));

        // Then: the validation fails
        assertTrue("Should be an error", response.isLeft());
        assertEquals(new Either.Left("Repository is not a directory"), response);
    }


    @Test
    public void test_when_there_is_no_pom_then_the_validation_fails() throws IOException
    {
        //Given: A repository
        Path repository = setupRepository();

        // When: validating for a non existant pom
        Either<String, Unit> response = subject.check(repository);

        // Then: the validation fails
        assertTrue("Should be an error", response.isLeft());
        assertEquals(new Either.Left("POM does not exist"), response);
    }


    @Test
    public void test_when_the_pom_has_no_protoc_plugin_then_the_validation_fails() throws IOException
    {
        //Given: A repository
        Path repository = setupRepository();
        //And: a pom
        createFile(repository, "pom.xml", EMPTY_POM);

        // When: validating for a pom without protoc plugin
        Either<String, Unit> response = subject.check(repository);

        // Then: the validation fails
        assertTrue("Should be an error", response.isLeft());
        assertEquals(new Either.Left("Has no protobuf-maven-plugin plugin"), response);
    }


    @Test
    public void test_when_the_pom_has_protoc_plugin_then_the_validation_succedes() throws IOException
    {
        //Given: A repository
        Path repository = setupRepository();
        //And: a pom
        createFile(repository, "pom.xml", PROTOC_POM);

        // When: validating
        Either<String, Unit> response = subject.check(repository);

        // Then: the validation succedes
        assertTrue("Should be a success", response.isRight());
    }


    private Path createFile(Path folder, String name, String content) throws IOException
    {
        Path file = Files.createFile(folder.resolve(name));
        FileOutputStream stream = new FileOutputStream(file.toFile());
        stream.write(content.getBytes());
        return file;
    }


    Path setupRepository() throws IOException
    {
        Path repositoryPath = Files.createTempDirectory("protocw-checker-test");
        repositoryPath.toFile().deleteOnExit();
        return repositoryPath;
    }

}