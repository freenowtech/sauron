package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.elasticsearch.DependenciesModel;
import com.freenow.sauron.plugins.elasticsearch.ElasticSearchClient;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Component;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

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
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

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

        assertDependencies(
            Map.of(
                "org.apache.tomcat.embed:tomcat-embed-websocket", "11.0.22",
                "org.apache.tomcat.embed:tomcat-embed-el", "11.0.22",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7", "2.3.21",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8", "1.3.50",
                "javax.annotation:javax_annotation-api", "1.3.2",
                "org.apache.tomcat.embed:tomcat-embed-core", "11.0.22",
                "org.jetbrains.kotlin:kotlin-stdlib", "2.3.21",
                "org.jetbrains:annotations", "13.0",
                "org.springframework.boot:spring-boot-starter-tomcat", "2.1.2.RELEASE"
            )
        );
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle", "build.gradle");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());

        assertDependencies(
            Map.of(
                "org.jetbrains.kotlin:kotlin-stdlib", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-common", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8", "1.3.61",
                "org.jetbrains:annotations", "13.0"
            )
        );
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithoutPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build-noplugin.gradle", "build.gradle");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());

        assertDependencies(
            Map.of(
                "org.jetbrains.kotlin:kotlin-stdlib", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-common", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8", "1.3.61",
                "org.jetbrains:annotations", "13.0"
            )
        );
    }


    @Test
    public void testDependencyCheckerGradleKotlinDsl() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle.kts", "build.gradle.kts");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_KOTLIN_DSL.toString());

        assertDependencies(
            Map.of(
                "org.jetbrains.kotlin:kotlin-stdlib", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-common", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7", "1.3.61",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8", "1.3.61",
                "org.jetbrains:annotations", "13.0"
            )
        );
    }


    @Test
    public void testDependencyCheckerNodeJsNpm() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "package-lock.json", "package-lock.json"
        ));

        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", NODEJS_NPM.toString());

        assertDependencies(
            Map.of(
                "org.npmjs:react", "18.0.0"
            )
        );
    }


    @Test
    public void testDependencyCheckerNodeJsYarn() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "yarn.lock", "yarn.lock"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", NODEJS_YARN.toString());

        assertDependencies(
            Map.of(
                "org.npmjs:react", "18.0.0",
                "org.npmjs:loose-envify", "1.4.0",
                "org.npmjs:js-tokens", "4.0.0"
            )
        );
    }


    @Test
    public void testDependencyCheckerNodeJsMissingPackageLockJson() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyNotPresent(dataSet, "cycloneDxBomPath");
        assertNoDependenciesReport();
    }


    @Test
    public void testDependencyCheckerPythonRequirementsProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("requirements.txt", "requirements.txt");
        dataSet = plugin.apply(createPythonPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", PYTHON_REQUIREMENTS.toString());

        assertDependencies(
            Map.of(
                "org.python:packaging", "21.3",
                "org.python:boto3", "1.17.105",
                "org.python:requests", "null",
                "org.python:eventlet", "null"
            )
        );
    }


    @Test
    public void testDependencyCheckerPythonPoetryProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("pyproject.toml", "pyproject.toml");
        dataSet = plugin.apply(createPythonPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", PYTHON_POETRY.toString());

        assertDependencies(
            Map.of(
                "org.python:packaging", "21.3",
                "org.python:boto3", "1.17.105",
                "org.python:s3transfer", "0.4.2",
                "org.python:urllib3", "1.26.20",
                "org.python:botocore", "1.20.112",
                "org.python:jmespath", "0.10.0",
                "org.python:six", "1.17.0",
                "org.python:python-dateutil", "2.9.0.post0",
                "org.python:pyparsing", "3.1.4"
            )
        );
    }


    @Test
    public void testDependencyCheckerSbtProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.sbt", "build.sbt");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", SBT.toString());
        assertNoDependenciesReport();
    }


    @Test
    public void testDependencyCheckerClojureProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("project.clj", "project.clj");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", CLOJURE.toString());
        assertNoDependenciesReport();
    }


    @Test
    public void testDependencyCheckerGoProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(
            Map.of("go-sbom/", "go-sbom")
        );
        dataSet = plugin.apply(createGoPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GO.toString());

        assertDependencies(
            Map.of(
                "org.golang:/wrk/go_mod", "null",
                "org.golang:gopkg_in/yaml_v2", "v2.4.0"
            )
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

        assertDependencies(
            Map.ofEntries(
                Map.entry("org.golang:/wrk/go_mod", "null"),
                Map.entry("org.golang:github_com/MicahParks/keyfunc", "v1.9.0"),
                Map.entry("org.golang:github_com/golang-jwt/jwt/v4", "v4.4.2"),
                Map.entry("org.golang:github_com/lestrrat-go/jwx/v2", "v2.1.4"),
                Map.entry("org.golang:github_com/prometheus/client_golang", "v1.21.1"),
                Map.entry("org.golang:github_com/stretchr/testify", "v1.10.0"),
                Map.entry("org.golang:gitlab_free-now_com/free-now/sre-backend/fnlog", "v0.6.0"),
                Map.entry("org.golang:github_com/beorn7/perks", "v1.0.1"),
                Map.entry("org.golang:github_com/cespare/xxhash/v2", "v2.3.0"),
                Map.entry("org.golang:github_com/davecgh/go-spew", "v1.1.1"),
                Map.entry("org.golang:github_com/decred/dcrd/dcrec/secp256k1/v4", "v4.4.0"),
                Map.entry("org.golang:github_com/goccy/go-json", "v0.10.3"),
                Map.entry("org.golang:github_com/klauspost/compress", "v1.17.11"),
                Map.entry("org.golang:github_com/kr/text", "v0.2.0"),
                Map.entry("org.golang:github_com/lestrrat-go/blackmagic", "v1.0.2"),
                Map.entry("org.golang:github_com/lestrrat-go/httpcc", "v1.0.1"),
                Map.entry("org.golang:github_com/lestrrat-go/httprc", "v1.0.6"),
                Map.entry("org.golang:github_com/lestrrat-go/iter", "v1.0.2"),
                Map.entry("org.golang:github_com/lestrrat-go/option", "v1.0.1"),
                Map.entry("org.golang:github_com/munnerz/goautoneg", "v0.0.0-20191010083416-a7dc8b61c822"),
                Map.entry("org.golang:github_com/pmezard/go-difflib", "v1.0.0"),
                Map.entry("org.golang:github_com/prometheus/client_model", "v0.6.1"),
                Map.entry("org.golang:github_com/prometheus/common", "v0.62.0"),
                Map.entry("org.golang:github_com/prometheus/procfs", "v0.15.1"),
                Map.entry("org.golang:github_com/segmentio/asm", "v1.2.0"),
                Map.entry("org.golang:golang_org/x/crypto", "v0.32.0"),
                Map.entry("org.golang:golang_org/x/sys", "v0.29.0"),
                Map.entry("org.golang:google_golang_org/protobuf", "v1.36.1"),
                Map.entry("org.golang:gopkg_in/yaml_v3", "v3.0.1")
            )
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


    private MockedConstruction<ElasticSearchClient> clientMockedConstruction;


    @Before
    public void mockElasticSearchClient()
    {
        clientMockedConstruction = mockConstruction(ElasticSearchClient.class);
    }


    @After
    public void unmockElasticSearchClient()
    {
        clientMockedConstruction.close();
    }


    private void assertDependencies(
        Map<String, String> expectedDependencies
    )
    {
        assertEquals(
            "Unexpected count of reports send to ElasticSearch",
            1,
            clientMockedConstruction.constructed().size()
        );
        ElasticSearchClient elasticSearchClient = clientMockedConstruction.constructed().get(0);
        ArgumentCaptor<DependenciesModel> captor = ArgumentCaptor.forClass(DependenciesModel.class);
        verify(elasticSearchClient).index(captor.capture());
        DependenciesModel dependenciesModel = captor.getValue();

        Map<String, Object> dependencies = dependenciesModel.getDependencies();
        Map<String, String> missingDependencies = new HashMap<>();
        Map<String, Pair<String, Object>> mismatchedDependencies = new HashMap<>();
        Map<String, Object> unexpectedDependencies = new HashMap<>();

        for (Map.Entry<String, String> dependency : expectedDependencies.entrySet())
        {
            if (!dependencies.containsKey(dependency.getKey()))
            {
                missingDependencies.put(dependency.getKey(), dependency.getValue());
            }
            else if (!dependency.getValue().equals(dependencies.get(dependency.getKey())))
            {
                mismatchedDependencies.put(dependency.getKey(), new ImmutablePair<>(dependency.getValue(), dependencies.get(dependency.getKey())));
            }
        }
        for (Map.Entry<String, Object> dependency : dependencies.entrySet())
        {
            String name = dependency.getKey().replaceAll("-(normalized|license)$", "");
            if (!name.equals("licenses") && !expectedDependencies.containsKey(name))
            {
                unexpectedDependencies.put(name, dependency.getValue());
            }
        }

        assertEquals(
            "Some dependencies weren't expected to be found",
            Map.of(),
            unexpectedDependencies
        );
        assertEquals(
            "Some expected dependencies weren't found",
            Map.of(),
            missingDependencies
        );
        assertEquals(
            "Some dependencies did not have the correct versions",
            Map.of(),
            mismatchedDependencies
        );
    }


    private void assertNoDependenciesReport()
    {
        assertEquals(
            "ElasticSearch was called unexpectedly",
            0,
            clientMockedConstruction.constructed().size()
        );
    }
}
