package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.freenow.sauron.plugins.ProjectType.CLOJURE;
import static com.freenow.sauron.plugins.ProjectType.GO;
import static com.freenow.sauron.plugins.ProjectType.GRADLE_GROOVY;
import static com.freenow.sauron.plugins.ProjectType.GRADLE_KOTLIN_DSL;
import static com.freenow.sauron.plugins.ProjectType.MAVEN;
import static com.freenow.sauron.plugins.ProjectType.NODEJS;
import static com.freenow.sauron.plugins.ProjectType.PYTHON_POETRY;
import static com.freenow.sauron.plugins.ProjectType.PYTHON_REQUIREMENTS;
import static com.freenow.sauron.plugins.ProjectType.SBT;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DependencyCheckerTest
{
    private final DependencyChecker plugin = new DependencyChecker();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public WireMockRule elasticsearchMock = new WireMockRule();


    @Before
    public void setUp()
    {
        elasticsearchMock.stubFor(
            post(urlPathMatching("/dependencies-\\d+/_doc")).willReturn(ok())
        );
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
        elasticsearchMock.verify(
            postRequestedFor(urlPathMatching("/dependencies-\\d+/_doc")).withRequestBody(equalToJson(
                "{\n" +
                    "  \"owner\": null,\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8-license\": \"\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8\": \"1.3.61\",\n" +
                    "  \"projectType\": \"GRADLE_GROOVY\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common-license\": \"\",\n" +
                    "  \"org.jetbrains:annotations-normalized\": \"0013.0000.0000\",\n" +
                    "  \"buildId\": null,\n" +
                    "  \"org.jetbrains:annotations-license\": \"Apache-2.0\",\n" +
                    "  \"commitId\": null,\n" +
                    "  \"serviceName\": null,\n" +
                    "  \"org.jetbrains:annotations\": \"13.0\",\n" +
                    "  \"environment\": \"none\",\n" +
                    "  \"licenses\": [\n" +
                    "    {\n" +
                    "      \"id\": \"Apache-2.0\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-normalized\": \"0001.0003.0061\",\n" +
                    "  \"eventTime\": null,\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-license\": \"\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7-license\": \"\"\n" +
                    "}"
            ))
        );
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithoutPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build-noplugin.gradle", "build.gradle");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());
        elasticsearchMock.verify(
            postRequestedFor(urlPathMatching("/dependencies-\\d+/_doc")).withRequestBody(equalToJson(
                "{\n" +
                    "  \"owner\": null,\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8-license\": \"\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8\": \"1.3.61\",\n" +
                    "  \"projectType\": \"GRADLE_GROOVY\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common-license\": \"\",\n" +
                    "  \"org.jetbrains:annotations-normalized\": \"0013.0000.0000\",\n" +
                    "  \"buildId\": null,\n" +
                    "  \"org.jetbrains:annotations-license\": \"Apache-2.0\",\n" +
                    "  \"commitId\": null,\n" +
                    "  \"serviceName\": null,\n" +
                    "  \"org.jetbrains:annotations\": \"13.0\",\n" +
                    "  \"environment\": \"none\",\n" +
                    "  \"licenses\": [\n" +
                    "    {\n" +
                    "      \"id\": \"Apache-2.0\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-normalized\": \"0001.0003.0061\",\n" +
                    "  \"eventTime\": null,\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-license\": \"\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7-license\": \"\"\n" +
                    "}"
            ))
        );
    }


    @Test
    public void testDependencyCheckerGradleKotlinDsl() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle.kts", "build.gradle.kts");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_KOTLIN_DSL.toString());
        elasticsearchMock.verify(
            postRequestedFor(urlPathMatching("/dependencies-\\d+/_doc")).withRequestBody(equalToJson(
                "{\n" +
                    "  \"owner\": null,\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8-license\": \"\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk8\": \"1.3.61\",\n" +
                    "  \"projectType\": \"GRADLE_KOTLIN_DSL\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common-license\": \"\",\n" +
                    "  \"org.jetbrains:annotations-normalized\": \"0013.0000.0000\",\n" +
                    "  \"buildId\": null,\n" +
                    "  \"org.jetbrains:annotations-license\": \"Apache-2.0\",\n" +
                    "  \"commitId\": null,\n" +
                    "  \"serviceName\": null,\n" +
                    "  \"org.jetbrains:annotations\": \"13.0\",\n" +
                    "  \"environment\": \"none\",\n" +
                    "  \"licenses\": [\n" +
                    "    {\n" +
                    "      \"id\": \"Apache-2.0\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-common\": \"1.3.61\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-normalized\": \"0001.0003.0061\",\n" +
                    "  \"eventTime\": null,\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7-normalized\": \"0001.0003.0061\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-license\": \"\",\n" +
                    "  \"org.jetbrains.kotlin:kotlin-stdlib-jdk7-license\": \"\"\n" +
                    "}"
            ))
        );
    }


    @Test
    public void testDependencyCheckerNodeJs() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "package-lock.json", "package-lock.json"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", NODEJS.toString());

        elasticsearchMock.verify(
            postRequestedFor(urlPathMatching("/dependencies-\\d+/_doc")).withRequestBody(equalToJson(
                "{\n" +
                    "  \"serviceName\" : null,\n" +
                    "  \"commitId\" : null,\n" +
                    "  \"eventTime\" : null,\n" +
                    "  \"buildId\" : null,\n" +
                    "  \"owner\" : null,\n" +
                    "  \"environment\" : \"none\",\n" +
                    "  \"projectType\" : \"NODEJS\",\n" +
                    "  \"licenses\" : [ ],\n" +
                    "  \"org.npmjs:react\" : \"18.0.0\",\n" +
                    "  \"org.npmjs:react-normalized\" : \"0018.0000.0000\",\n" +
                    "  \"org.npmjs:react-license\" : \"\"\n" +
                    "}"
            ))
        );
    }


    @Test
    public void testDependencyCheckerNodeJsYarnNotSupported() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "yarn.lock", "yarn.lock"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyNotPresent(dataSet, "cycloneDxBomPath");
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

        elasticsearchMock.verify(
            postRequestedFor(urlPathMatching("/dependencies-\\d+/_doc")).withRequestBody(equalToJson(
                "{\n" +
                    "  \"owner\" : null,\n" +
                    "  \"org.python:packaging-normalized\" : \"0021.0003.0000\",\n" +
                    "  \"projectType\" : \"PYTHON_REQUIREMENTS\",\n" +
                    "  \"buildId\" : null,\n" +
                    "  \"commitId\" : null,\n" +
                    "  \"serviceName\" : null,\n" +
                    "  \"environment\" : \"none\",\n" +
                    "  \"licenses\" : [ ],\n" +
                    "  \"org.python:packaging-license\" : \"\",\n" +
                    "  \"org.python:requests\" : \"null\",\n" +
                    "  \"org.python:eventlet-normalized\" : \"0000.0000.0000\",\n" +
                    "  \"org.python:packaging\" : \"21.3\",\n" +
                    "  \"org.python:boto3\" : \"1.17.105\",\n" +
                    "  \"org.python:boto3-normalized\" : \"0001.0017.0105\",\n" +
                    "  \"org.python:eventlet-license\" : \"\",\n" +
                    "  \"eventTime\" : null,\n" +
                    "  \"org.python:eventlet\" : \"null\",\n" +
                    "  \"org.python:requests-normalized\" : \"0000.0000.0000\",\n" +
                    "  \"org.python:boto3-license\" : \"\",\n" +
                    "  \"org.python:requests-license\" : \"\"\n" +
                    "}"
            ))
        );
    }


    @Test
    public void testDependencyCheckerPythonPoetryProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("pyproject.toml", "pyproject.toml");
        dataSet = plugin.apply(createPythonPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", PYTHON_POETRY.toString());

        elasticsearchMock.verify(
            postRequestedFor(urlPathMatching("/dependencies-\\d+/_doc")).withRequestBody(equalToJson(
                "{\n" +
                    "  \"org.python:s3transfer\" : \"0.4.2\",\n" +
                    "  \"org.python:urllib3-normalized\" : \"0001.0026.0020\",\n" +
                    "  \"org.python:botocore-normalized\" : \"0001.0020.0112\",\n" +
                    "  \"org.python:six-normalized\" : \"0001.0017.0000\",\n" +
                    "  \"org.python:botocore\" : \"1.20.112\",\n" +
                    "  \"org.python:packaging-normalized\" : \"0021.0003.0000\",\n" +
                    "  \"org.python:python-dateutil-license\" : \"\",\n" +
                    "  \"projectType\" : \"PYTHON_POETRY\",\n" +
                    "  \"org.python:jmespath\" : \"0.10.0\",\n" +
                    "  \"org.python:python-dateutil-normalized\" : \"0002.0009.0000\",\n" +
                    "  \"org.python:jmespath-license\" : \"\",\n" +
                    "  \"org.python:boto3\" : \"1.17.105\",\n" +
                    "  \"org.python:boto3-normalized\" : \"0001.0017.0105\",\n" +
                    "  \"org.python:jmespath-normalized\" : \"0000.0010.0000\",\n" +
                    "  \"eventTime\" : null,\n" +
                    "  \"org.python:pyparsing-normalized\" : \"0003.0001.0004\",\n" +
                    "  \"org.python:six-license\" : \"\",\n" +
                    "  \"org.python:boto3-license\" : \"\",\n" +
                    "  \"org.python:python-dateutil\" : \"2.9.0.post0\",\n" +
                    "  \"org.python:urllib3-license\" : \"\",\n" +
                    "  \"owner\" : null,\n" +
                    "  \"org.python:urllib3\" : \"1.26.20\",\n" +
                    "  \"org.python:pyparsing-license\" : \"\",\n" +
                    "  \"buildId\" : null,\n" +
                    "  \"org.python:botocore-license\" : \"\",\n" +
                    "  \"commitId\" : null,\n" +
                    "  \"serviceName\" : null,\n" +
                    "  \"environment\" : \"none\",\n" +
                    "  \"licenses\" : [ ],\n" +
                    "  \"org.python:packaging-license\" : \"\",\n" +
                    "  \"org.python:packaging\" : \"21.3\",\n" +
                    "  \"org.python:s3transfer-normalized\" : \"0000.0004.0002\",\n" +
                    "  \"org.python:s3transfer-license\" : \"\",\n" +
                    "  \"org.python:six\" : \"1.17.0\",\n" +
                    "  \"org.python:pyparsing\" : \"3.1.4\"\n" +
                    "}"
            ))
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
        DataSet dataSet = createDataSet("go.mod", "go.mod");
        dataSet = plugin.apply(pluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GO.toString());
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
                Files.copy(testFile, tempTestFile);
            }
        }

        dataSet.setAdditionalInformation("repositoryPath", tempFolderPath.toString());
        return dataSet;
    }


    private PluginsConfigurationProperties createNodeJsPluginConfigurationProperties()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        PluginsConfigurationProperties properties = pluginConfigurationProperties();
        properties.get("dependency-checker").put(
            "nodejs", Map.of(
                "npm", Objects.requireNonNull(classLoader.getResource("bin/npm")).getPath()
            )
        );
        return properties;
    }


    private PluginsConfigurationProperties createPythonPluginConfigurationProperties()
    {
        return pluginConfigurationProperties();
    }


    private PluginsConfigurationProperties pluginConfigurationProperties()
    {
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put(
            "dependency-checker", new HashMap<>(Map.of(
                "elasticsearch", Map.of(
                    "host", "localhost",
                    "port", elasticsearchMock.port(),
                    "scheme", "http"
                )
            ))
        );

        return properties;
    }
}
