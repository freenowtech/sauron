package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.freenow.sauron.plugins.ProjectType.CLOJURE;
import static com.freenow.sauron.plugins.ProjectType.GRADLE_GROOVY;
import static com.freenow.sauron.plugins.ProjectType.GRADLE_KOTLIN_DSL;
import static com.freenow.sauron.plugins.ProjectType.MAVEN;
import static com.freenow.sauron.plugins.ProjectType.NODEJS;
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
    public final TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void testDependencyCheckerMavenProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("pom.xml", "pom.xml");
        dataSet = plugin.apply(new PluginsConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", MAVEN.toString());
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle", "build.gradle");
        dataSet = plugin.apply(new PluginsConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());
        checkKeyPresent(plugin.dependenciesModel, "org.jetbrains.kotlin:kotlin-stdlib", "1.3.61");
    }


    @Test
    public void testDependencyCheckerGradleGroovyProjectWithoutPlugins() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build-noplugin.gradle", "build.gradle");
        dataSet = plugin.apply(new PluginsConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_GROOVY.toString());
        checkKeyPresent(plugin.dependenciesModel, "org.jetbrains.kotlin:kotlin-stdlib", "1.3.61");
    }


    @Test
    public void testDependencyCheckerGradleKotlinDsl() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.gradle.kts", "build.gradle.kts");
        dataSet = plugin.apply(new PluginsConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", GRADLE_KOTLIN_DSL.toString());
        checkKeyPresent(plugin.dependenciesModel, "org.jetbrains.kotlin:kotlin-stdlib", "1.3.61");
    }


    @Ignore
    @Test
    public void testDependencyCheckerNodeJs() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet(Map.of(
            "package.json", "package.json",
            "package-lock.json", "package-lock.json"
        ));
        dataSet = plugin.apply(createNodeJsPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", NODEJS.toString());
        // includes production dependencies
        checkKeyPresent(plugin.dependenciesModel, "org.npmjs:react", "18.0.0");
        // excludes development dependencies
        checkKeyNotPresent(plugin.dependenciesModel, "org.npmjs:@testing-library/react");
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

        checkKeyPresent(plugin.dependenciesModel, "org.python:boto3", "1.17.105");
    }


    @Test
    public void testDependencyCheckerPythonPoetryProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("pyproject.toml", "pyproject.toml");
        dataSet = plugin.apply(createPythonPluginConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", PYTHON_POETRY.toString());

        checkKeyPresent(plugin.dependenciesModel, "org.python:boto3", "1.17.105");
    }


    @Test
    public void testDependencyCheckerSbtProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("build.sbt", "build.sbt");
        dataSet = plugin.apply(new PluginsConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", SBT.toString());
    }


    @Test
    public void testDependencyCheckerClojureProject() throws IOException, URISyntaxException
    {
        DataSet dataSet = createDataSet("project.clj", "project.clj");
        dataSet = plugin.apply(new PluginsConfigurationProperties(), dataSet);
        checkKeyPresent(dataSet, "projectType", CLOJURE.toString());
    }


    private void checkKeyPresent(DataSet dataSet, String key, Object expected)
    {
        assertNotNull(dataSet);
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }


    private void checkKeyPresent(Map<String, Object> dataSet, String key, Object expected)
    {
        assertNotNull(dataSet);
        assertTrue(dataSet.containsKey(key));
        assertEquals(expected, dataSet.get(key));
    }


    private void checkKeyNotPresent(DataSet dataSet, String key)
    {
        assertNotNull(dataSet);
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertFalse(response.isPresent());
    }


    private void checkKeyNotPresent(Map<String, Object> dataSet, String key)
    {
        assertNotNull(dataSet);
        assertFalse(dataSet.containsKey(key));
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
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put("dependency-checker", Map.of(
            "nodejs", Map.of(
                "npm", Objects.requireNonNull(classLoader.getResource("bin/npm")).getPath(),
                "npx", Objects.requireNonNull(classLoader.getResource("bin/npx")).getPath()
            )
        ));
        return properties;
    }


    private PluginsConfigurationProperties createPythonPluginConfigurationProperties()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        PluginsConfigurationProperties properties = new PluginsConfigurationProperties();
        properties.put("dependency-checker", Map.of(
            "python", Map.of(
                "path", Objects.requireNonNull(classLoader.getResource("bin/python")).getPath()
            )
        ));
        return properties;
    }
}