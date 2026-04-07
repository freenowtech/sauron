package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import org.apache.commons.io.FileUtils;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.XmlParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.freenow.sauron.plugins.ProjectType.CLOJURE;
import static com.freenow.sauron.plugins.ProjectType.GO;
import static com.freenow.sauron.plugins.ProjectType.GRADLE_GROOVY;
import static com.freenow.sauron.plugins.ProjectType.GRADLE_KOTLIN_DSL;
import static com.freenow.sauron.plugins.ProjectType.MAVEN;
import static com.freenow.sauron.plugins.ProjectType.NODEJS_NPM;
import static com.freenow.sauron.plugins.ProjectType.NODEJS_YARN;
import static com.freenow.sauron.plugins.ProjectType.PYTHON_POETRY;
import static com.freenow.sauron.plugins.ProjectType.PYTHON_REQUIREMENTS;
import static com.freenow.sauron.plugins.ProjectType.SBT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DependencyCheckerTest
{
    private final DependencyChecker plugin = new DependencyChecker();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));


    @Before
    public void ensureDockerImageExists()
    {
        assertTrue("Docker is required for this test", isDockerAvailable());
        try
        {
            Process process = new ProcessBuilder(
                "docker", "image", "inspect", "sauron-tooling:latest"
            )
                .redirectErrorStream(true)
                .start();

            int exitCode = process.waitFor();

            if (exitCode != 0)
            {
                throw new IllegalStateException(
                    "Docker image 'sauron-tooling:latest' does not exist. " +
                        "Run: docker build -f ../../sauron-service/Dockerfile --target sauron-tooling -t sauron-tooling:latest ../../sauron-service"
                );
            }

        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to verify Docker image existence", e);
        }
    }


    @Test
    public void testDependencyCheckerMavenProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("pom.xml", "pom.xml");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", MAVEN.toString());
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle", "build.gradle");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());

        Path bomXmlPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomXmlPath, Files.exists(bomXmlPath));
        Bom bom = parseBomXmlFromFile(bomXmlPath);
        assertTrue(
            "kotlin-stdlib-jdk8@1.3.61 should be present in bom.xml",
            hasDependency(bom, "org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.3.61")
        );
        assertTrue(
            "BOM should contain all direct dependencies from build.gradle and their transitive dependencies identified by Gradle",
            bom.getComponents().stream().anyMatch(c -> c.getType() == Component.Type.LIBRARY)
        );
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithoutPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build-noplugin.gradle", "build.gradle");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());

        Path bomXmlPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomXmlPath, Files.exists(bomXmlPath));
        Bom bom = parseBomXmlFromFile(bomXmlPath);
        assertTrue(
            "kotlin-stdlib-jdk8@1.3.61 should be present in bom.xml",
            hasDependency(bom, "org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.3.61")
        );
        assertTrue(
            "BOM should contain all direct dependencies from build-noplugin.gradle and their transitive dependencies identified by Gradle",
            bom.getComponents().stream().anyMatch(c -> c.getType() == Component.Type.LIBRARY)
        );
    }


    @Test
    public void testDependencyCheckerGradleKotlinDsl() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle.kts", "build.gradle.kts");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_KOTLIN_DSL.toString());

        Path bomXmlPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomXmlPath, Files.exists(bomXmlPath));
        Bom bom = parseBomXmlFromFile(bomXmlPath);
        assertTrue(
            "kotlin-stdlib-jdk8@1.3.61 should be present in bom.xml",
            hasDependency(bomXmlPath, "org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.3.61")
        );
        assertTrue(
            "BOM should contain all direct dependencies from build.gradle.kts and their transitive dependencies identified by Gradle",
            bom.getComponents().stream().anyMatch(c -> c.getType() == Component.Type.LIBRARY)
        );
    }


    @Test
    public void testDependencyCheckerNodeJsNpm() throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "package-lock.json", "package-lock.json"
        ));

        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", NODEJS_NPM.toString());

        Path bomJsonPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomJsonPath, Files.exists(bomJsonPath));
        assertTrue("react@18.0.0 should be present in bom.json", hasJsonDependency(bomJsonPath, "react", "18.0.0"));
        assertEquals("BOM should contain 1 library component (react)", 1, invokeParseCycloneDxJson(plugin, bomJsonPath).size());
    }


    @Test
    public void testDependencyCheckerNodeJsYarn() throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "yarn.lock", "yarn.lock"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", NODEJS_YARN.toString());

        Path bomJsonPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomJsonPath, Files.exists(bomJsonPath));
        Map<String, String> dependencies = Map.of(
            "react", "18.0.0",
            "loose-envify", "1.4.0",
            "js-tokens", "4.0.0"
        );
        for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
            assertTrue(dependency.getKey() + "@" + dependency.getValue() + " should be present in bom.json", hasJsonDependency(bomJsonPath, dependency.getKey(), dependency.getValue()));
        }
        assertEquals("BOM should contain 1 library component (react)", dependencies.size(), invokeParseCycloneDxJson(plugin, bomJsonPath).size());
    }


    @Test
    public void testDependencyCheckerNodeJsMissingPackageLockJson() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyNotPresent(dataSet, "cycloneDxBomPath");
    }


    @Test
    public void testDependencyCheckerPythonRequirementsProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("requirements.txt", "requirements.txt");
        dataSet = plugin.apply(createPythonPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", PYTHON_REQUIREMENTS.toString());

        Path bomXmlPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomXmlPath, Files.exists(bomXmlPath));
        Bom bom = parseBomXmlFromFile(bomXmlPath);
        assertTrue(
            "packaging==21.3 should be present in bom.xml",
            hasDependency(bom, null, "packaging", "21.3")
        );
        assertTrue(
            "boto3==1.17.105 should be present in bom.xml",
            hasDependency(bom, null, "boto3", "1.17.105")
        );
        assertTrue(
            "requests should be present in bom.xml",
            hasDependency(bom, null, "requests", null)
        );
        assertTrue(
            "eventlet should be present in bom.xml",
            hasDependency(bom, null, "eventlet", null)
        );
        assertTrue(
            "eventlet should be present in bom.xml",
            hasDependency(bom, null, "eventlet", null)
        );
        assertEquals(
            "Should have same number of dependencies",
            4, bom.getComponents().stream().filter(c -> c.getType() == Component.Type.LIBRARY).count()
        );
    }


    @Test
    public void testDependencyCheckerPythonPoetryProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("pyproject.toml", "pyproject.toml");
        dataSet = plugin.apply(createPythonPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", PYTHON_POETRY.toString());

        Path bomXmlPath = Paths.get((String) dataSet.getObjectAdditionalInformation("cycloneDxBomPath").orElseThrow());
        assertTrue("BOM file should exist at " + bomXmlPath, Files.exists(bomXmlPath));
        Bom bom = parseBomXmlFromFile(bomXmlPath);
        assertTrue(
            "packaging=21.3 should be present in bom.xml",
            hasDependency(bom, null, "packaging", "21.3")
        );
        assertTrue(
            "boto3=1.17.105 should be present in bom.xml",
            hasDependency(bom, null, "boto3", "1.17.105")
        );
        assertTrue(
            "BOM should contain all direct dependencies from pyproject.toml and their transitive dependencies identified by Poetry",
            bom.getComponents().stream().filter(c -> c.getType() == Component.Type.LIBRARY).count() >= 4
        );
    }


    @Test
    public void testDependencyCheckerSbtProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.sbt", "build.sbt");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", SBT.toString());
    }


    @Test
    public void testDependencyCheckerClojureProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("project.clj", "project.clj");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", CLOJURE.toString());
    }


    @Test
    public void testDependencyCheckerGoProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(
            Map.of("go-sbom/", "go-sbom")
        );
        dataSet = plugin.apply(createGoPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GO.toString());
        Path bomXmlPath = tempFolder.getRoot().toPath().resolve("go-sbom/bom.xml");
        checkKeyPresent(dataSet, "cycloneDxBomPath", bomXmlPath.toString());

        Bom bom = parseBomXmlFromFile(bomXmlPath);
        assertTrue(
            "yaml.v2@v2.4.0 should be present in bom.xml",
            hasDependency(bom, null, "gopkg.in/yaml.v2", "v2.4.0")
        );
        assertEquals(
            "Should have same number of dependencies",
            1, bom.getComponents().stream().filter(c -> c.getType() == Component.Type.LIBRARY).count()
        );
    }


    @Test
    public void testDependencyCheckerGoSubFolderProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(
            Map.of("go-sbom-sub/", "go-sbom-sub/")
        );
        dataSet = plugin.apply(createGoPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GO.toString());
        Path bomXmlPath = tempFolder.getRoot().toPath().resolve("go-sbom-sub/dummys/bom.xml");
        checkKeyPresent(dataSet, "cycloneDxBomPath", bomXmlPath.toString());

        Bom bom = parseBomXmlFromFile(bomXmlPath);
        Map<String, String> expectedDeps = Map.ofEntries(
            Map.entry("github.com/MicahParks/keyfunc", "v1.9.0"),
            Map.entry("github.com/golang-jwt/jwt/v4", "v4.4.2"),
            Map.entry("github.com/lestrrat-go/jwx/v2", "v2.1.4"),
            Map.entry("github.com/prometheus/client_golang", "v1.21.1"),
            Map.entry("github.com/stretchr/testify", "v1.10.0"),
            Map.entry("gitlab.free-now.com/free-now/sre-backend/fnlog", "v0.6.0"),
            Map.entry("github.com/beorn7/perks", "v1.0.1"),
            Map.entry("github.com/cespare/xxhash/v2", "v2.3.0"),
            Map.entry("github.com/davecgh/go-spew", "v1.1.1"),
            Map.entry("github.com/decred/dcrd/dcrec/secp256k1/v4", "v4.4.0"),
            Map.entry("github.com/goccy/go-json", "v0.10.3"),
            Map.entry("github.com/klauspost/compress", "v1.17.11"),
            Map.entry("github.com/kr/text", "v0.2.0"),
            Map.entry("github.com/lestrrat-go/blackmagic", "v1.0.2"),
            Map.entry("github.com/lestrrat-go/httpcc", "v1.0.1"),
            Map.entry("github.com/lestrrat-go/httprc", "v1.0.6"),
            Map.entry("github.com/lestrrat-go/iter", "v1.0.2"),
            Map.entry("github.com/lestrrat-go/option", "v1.0.1"),
            Map.entry("github.com/munnerz/goautoneg", "v0.0.0-20191010083416-a7dc8b61c822"),
            Map.entry("github.com/pmezard/go-difflib", "v1.0.0"),
            Map.entry("github.com/prometheus/client_model", "v0.6.1"),
            Map.entry("github.com/prometheus/common", "v0.62.0"),
            Map.entry("github.com/prometheus/procfs", "v0.15.1"),
            Map.entry("github.com/segmentio/asm", "v1.2.0"),
            Map.entry("golang.org/x/crypto", "v0.32.0"),
            Map.entry("golang.org/x/sys", "v0.29.0"),
            Map.entry("google.golang.org/protobuf", "v1.36.1"),
            Map.entry("gopkg.in/yaml.v3", "v3.0.1")
        );

        expectedDeps.forEach((name, version) ->
            assertTrue(
                String.format("%s@%s should be present in bom.xml", name, version),
                hasDependency(bom, null, name, version)
            )
        );
        assertEquals(
            "Should have same number of dependencies",
            expectedDeps.size(),
            bom.getComponents().stream().filter(c -> c.getType() == Component.Type.LIBRARY).count()
        );
    }


    @Test
    public void testParseCycloneDxJsonWithInvalidSerialNumber() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // Given
        String invalidBomContent = "{\n" +
            "  \"bomFormat\": \"CycloneDX\",\n" +
            "  \"specVersion\": \"1.4\",\n" +
            "  \"serialNumber\": \"urn:uuid:***\",\n" +
            "  \"version\": 1,\n" +
            "  \"components\": [\n" +
            "    {\n" +
            "      \"type\": \"library\",\n" +
            "      \"name\": \"react\",\n" +
            "      \"version\": \"18.2.0\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        Path bomJson = tempFolder.getRoot().toPath().resolve("bom.json");
        Files.write(bomJson, invalidBomContent.getBytes(StandardCharsets.UTF_8));

        // When
        List<Component> components = invokeParseCycloneDxJson(plugin, bomJson);

        // Then
        assertNotNull(components);
        assertEquals(1, components.size());
        assertEquals("react", components.get(0).getName());
        assertEquals("18.2.0", components.get(0).getVersion());

        String sanitizedBomContent = Files.readString(bomJson);
        assertFalse("The serialNumber should have been sanitized", sanitizedBomContent.contains("***"));
    }


    @Test
    public void testParseCycloneDxJsonWithValidBom() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // Given
        String validBomContent = "{\n" +
            "  \"bomFormat\": \"CycloneDX\",\n" +
            "  \"specVersion\": \"1.4\",\n" +
            "  \"serialNumber\": \"urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79\",\n" +
            "  \"version\": 1,\n" +
            "  \"components\": [\n" +
            "    {\n" +
            "      \"type\": \"library\",\n" +
            "      \"name\": \"express\",\n" +
            "      \"version\": \"4.18.2\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        Path bomJson = tempFolder.getRoot().toPath().resolve("bom.json");
        Files.write(bomJson, validBomContent.getBytes(StandardCharsets.UTF_8));

        // When
        List<Component> components = invokeParseCycloneDxJson(plugin, bomJson);

        // Then
        assertNotNull(components);
        assertEquals(1, components.size());
        assertEquals("express", components.get(0).getName());
        assertEquals("4.18.2", components.get(0).getVersion());

        String bomContent = new String(Files.readAllBytes(bomJson), StandardCharsets.UTF_8);
        assertEquals("The BOM file should not be modified", validBomContent, bomContent);
    }


    @Test
    public void testParseCycloneDxJsonWithNoComponents() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // Given
        String bomWithNoComponents = "{\n" +
            "  \"bomFormat\": \"CycloneDX\",\n" +
            "  \"specVersion\": \"1.4\",\n" +
            "  \"serialNumber\": \"urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79\",\n" +
            "  \"version\": 1,\n" +
            "  \"components\": []\n" +
            "}";

        Path bomJson = tempFolder.getRoot().toPath().resolve("bom.json");
        Files.write(bomJson, bomWithNoComponents.getBytes(StandardCharsets.UTF_8));

        // When
        List<Component> components = invokeParseCycloneDxJson(plugin, bomJson);

        // Then
        assertNotNull(components);
        assertTrue(components.isEmpty());
    }


    @SuppressWarnings("unchecked")
    private List<Component> invokeParseCycloneDxJson(DependencyChecker plugin, Path bom) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method method = DependencyChecker.class.getDeclaredMethod("parseCycloneDxJson", Path.class);
        method.setAccessible(true);
        return (List<Component>) method.invoke(plugin, bom);
    }


    private void checkKeyPresent(DataSet dataSet, String key, Object expected)
    {
        assertNotNull(dataSet);
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }


    private void checkKeyNotPresent(DataSet dataSet, String key)
    {
        assertNotNull(dataSet);
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertFalse(response.isPresent());
    }


    private DataSet createDataSet(String testFilename, String targetFileName) throws IOException, URISyntaxException
    {
        return createDataSet(Map.of(testFilename, targetFileName));
    }


    private DataSet createDataSet(Map<String, String> fileMappings) throws IOException, URISyntaxException
    {
        DataSet dataSet = new DataSet();
        Path tempFolderPath = tempFolder.getRoot().toPath();

        for (Map.Entry<String, String> entry : fileMappings.entrySet())
        {
            String source = entry.getKey();
            String target = entry.getValue();

            if (source != null && !source.isEmpty())
            {
                ClassLoader classLoader = getClass().getClassLoader();
                Path testFile = Paths.get(Objects.requireNonNull(classLoader.getResource(source)).toURI());
                Path tempTestFile = tempFolderPath.resolve(target);

                if (Files.isDirectory(testFile))
                {
                    FileUtils.copyDirectory(testFile.toFile(), tempTestFile.toFile());
                }
                else
                {
                    Files.createDirectories(tempTestFile.getParent());
                    Files.copy(testFile, tempTestFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        dataSet.setAdditionalInformation("repositoryPath", tempFolderPath.toString());
        return dataSet;
    }


    private PluginsConfigurationProperties createNodeJsPluginConfigurationProperties()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        PluginsConfigurationProperties properties = pluginConfigurationProperties();
        try
        {
            properties.get("dependency-checker").put(
                "nodejs", Map.of(
                    "npm", Paths.get(Objects.requireNonNull(classLoader.getResource("bin/npm"), "Resource 'bin/npm' not found").toURI()).toString(),
                    "corepack", Paths.get(Objects.requireNonNull(classLoader.getResource("bin/corepack"), "Resource 'bin/corepack' not found").toURI()).toString()
                )
            );
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException("Failed to resolve resource path", e);
        }
        return properties;

    }


    private PluginsConfigurationProperties createPythonPluginConfigurationProperties()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        PluginsConfigurationProperties properties = pluginConfigurationProperties();
        try
        {
            properties.get("dependency-checker").put(
                "python", Map.of(
                    "path", Paths.get(Objects.requireNonNull(classLoader.getResource("bin/python"), "Resource 'bin/python' not found").toURI()).toString(),
                    "poetry", Paths.get(Objects.requireNonNull(classLoader.getResource("bin/poetry"), "Resource 'bin/poetry' not found").toURI()).toString(),
                    "cyclonedx-py", Paths.get(Objects.requireNonNull(classLoader.getResource("bin/cyclonedx-py"), "Resource 'bin/cyclonedx-py' not found").toURI()).toString()
                )
            );
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException("Failed to resolve resource path", e);
        }
        return properties;
    }


    private PluginsConfigurationProperties createGoPluginConfigurationProperties()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        PluginsConfigurationProperties properties = pluginConfigurationProperties();
        try
        {
            properties.get("dependency-checker").put(
                "go", Map.of(
                    "syft",
                    Paths.get(Objects.requireNonNull(classLoader.getResource("bin/syft"), "Resource 'bin/syft' not found").toURI()).toString()
                )
            );
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException("Failed to resolve resource path", e);
        }
        return properties;
    }


    private PluginsConfigurationProperties pluginConfigurationProperties()
    {
        return new PluginsConfigurationProperties()
        {{
            put("dependency-checker", new HashMap<>());
        }};
    }


    private boolean hasJsonDependency(Path bomJsonPath, String name, String version) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        List<Component> components = invokeParseCycloneDxJson(plugin, bomJsonPath);
        return components.stream()
            .anyMatch(c -> name.equals(c.getName()) && version.equals(c.getVersion()));
    }


    private boolean hasDependency(Path bomXmlPath, String group, String name, String version)
    {
        return hasDependency(parseBomXmlFromFile(bomXmlPath), group, name, version);
    }


    private boolean hasDependency(Bom bom, String group, String name, String version)
    {
        if (bom == null || bom.getComponents() == null)
        {
            return false;
        }
        return bom.getComponents().stream().anyMatch(c -> matches(c, group, name, version));
    }


    private boolean matches(Component component, String group, String name, String version)
    {
        return (group == null || Objects.equals(group, component.getGroup())) &&
            Objects.equals(name, component.getName()) &&
            Objects.equals(version, component.getVersion());
    }


    private Bom parseBomXmlFromFile(Path bomXmlPath)
    {
        try
        {
            return new XmlParser().parse(bomXmlPath.toFile());
        }
        catch (ParseException e)
        {
            throw new IllegalStateException("Failed to parse BOM file: " + bomXmlPath, e);
        }
    }


    private boolean isDockerAvailable()
    {
        try
        {
            Process process = Runtime.getRuntime().exec("docker ps");
            return process.waitFor() == 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
