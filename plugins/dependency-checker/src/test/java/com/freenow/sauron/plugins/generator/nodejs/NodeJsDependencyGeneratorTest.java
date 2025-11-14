package com.freenow.sauron.plugins.generator.nodejs;

import com.freenow.sauron.plugins.command.Command;
import com.freenow.sauron.plugins.command.NonZeroExitCodeException;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class NodeJsDependencyGeneratorTest
{
    @TempDir
    Path tempDir;

    private MockedStatic<Command> commandMockedStatic;
    private Command command;

    @BeforeEach
    void setup() throws IOException
    {
        Files.createFile(tempDir.resolve("package-lock.json"));

        commandMockedStatic = Mockito.mockStatic(Command.class);
        Command.CommandBuilder commandBuilder = Mockito.mock(Command.CommandBuilder.class);
        command = Mockito.mock(Command.class);

        commandMockedStatic.when(Command::builder).thenReturn(commandBuilder);

        Mockito.when(commandBuilder.commandTimeout(any())).thenReturn(commandBuilder);
        Mockito.when(commandBuilder.repositoryPath(any())).thenReturn(commandBuilder);
        Mockito.when(commandBuilder.commandline(any())).thenReturn(commandBuilder);
        Mockito.when(commandBuilder.outputFile(any())).thenReturn(commandBuilder);
        Mockito.when(commandBuilder.build()).thenReturn(command);
    }

    @AfterEach
    void tearDown()
    {
        commandMockedStatic.close();
    }

    @Test
    void testGenerateCycloneDxBomWithInvalidSerialNumberIsFixed() throws IOException, InterruptedException, NonZeroExitCodeException
    {
        // Given
        Mockito.doAnswer(invocation -> {
            Path bomJson = tempDir.resolve("bom.json");
            String bomContent = "{\n" +
                "  \"bomFormat\": \"CycloneDX\",\n" +
                "  \"specVersion\": \"1.4\",\n" +
                "  \"serialNumber\": \"urn:uuid:***\",\n" +
                "  \"version\": 1,\n" +
                "  \"components\": []\n" +
                "}";
            Files.write(bomJson, bomContent.getBytes());
            return null;
        }).when(command).run();

        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        NodeJsDependencyGenerator generator = new NodeJsDependencyGenerator(properties);

        // When
        Path cycloneDxBom = generator.generateCycloneDxBom(tempDir);

        // Then
        assertTrue(Files.exists(cycloneDxBom));
        String bomContent = new String(Files.readAllBytes(cycloneDxBom));
        assertFalse(bomContent.contains("***"));
        assertTrue(Pattern.compile("\"serialNumber\": \"urn:uuid:[a-f0-9\\-]{36}\"").matcher(bomContent).find());
    }

    @Test
    void testGenerateCycloneDxBomWithValidSerialNumberIsNotModified() throws IOException, InterruptedException, NonZeroExitCodeException
    {
        // Given
        String validSerialNumberLine = "\"serialNumber\": \"urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79\"";
        Mockito.doAnswer(invocation -> {
            Path bomJson = tempDir.resolve("bom.json");
            String bomContent = "{\n" +
                "  \"bomFormat\": \"CycloneDX\",\n" +
                "  \"specVersion\": \"1.4\",\n" +
                "  " + validSerialNumberLine + ",\n" +
                "  \"version\": 1,\n" +
                "  \"components\": []\n" +
                "}";
            Files.write(bomJson, bomContent.getBytes());
            return null;
        }).when(command).run();

        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        NodeJsDependencyGenerator generator = new NodeJsDependencyGenerator(properties);

        // When
        Path cycloneDxBom = generator.generateCycloneDxBom(tempDir);

        // Then
        assertTrue(Files.exists(cycloneDxBom));
        String bomContent = new String(Files.readAllBytes(cycloneDxBom));
        assertTrue(bomContent.contains(validSerialNumberLine));
    }
}
