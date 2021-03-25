package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenReportTest
{

    private final MavenReport plugin = new MavenReport();

    @Test
    public void testDependencyExist()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "direct-dependency-check",
            Collections.singletonMap(
                "is_kotlin",
                "org.jetbrains.kotlin:kotlin-stdlib")));
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "is_kotlin", true);
    }


    @Test
    public void testDependencyNotExist()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "direct-dependency-check",
            Collections.singletonMap(
                "use_mockito",
                "org.mockito:mockito-core")));
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "use_mockito", false);
    }


    @Test
    public void testDependencyWithScopeExist()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "direct-dependency-check",
            Collections.singletonMap(
                "use_boot_starter_tomcat",
                "org.springframework.boot:spring-boot-starter-tomcat:provided")));
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "use_boot_starter_tomcat", true);
    }


    @Test
    public void testDependencyWithScopeNotExist()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "direct-dependency-check",
            Collections.singletonMap(
                "is_kotlin",
                "org.jetbrains.kotlin:kotlin-stdlib:test")));
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "is_kotlin", false);
    }


    @Test
    public void testAdvancedSearchParentInformation()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "advanced-search-check",
            new HashMap<String, String>() {{
                put("parentGroupId", "/project/parent/groupId");
                put("parentArtifactId", "/project/parent/artifactId");
                put("parentVersion", "/project/parent/version");
                put("parentRelativePath", "/project/parent/relativePath");
            }}
        ));
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "parentGroupId", "com.test");
        checkKeyPresent(dataSet, "parentArtifactId", "parent");
        checkKeyPresent(dataSet, "parentVersion", "1.1.0");
        checkKeyPresent(dataSet, "parentRelativePath", "../pom.xml");

    }


    @Test
    public void testAdvancedSearchJavaVersion()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "advanced-search-check",
            Collections.singletonMap(
                "javaVersion", "/project/properties/java.version"
            ))
        );
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "javaVersion", "1.8");
    }


    @Test
    public void testAdvancedSearchNonExistingKey()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "advanced-search-check",
            Collections.singletonMap(
                "existsPackaging", "boolean(/project/packaging)"
            ))
        );
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "existsPackaging", "false");
    }


    @Test
    public void testAdvancedSearchExistingKey()
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = createPluginsConfigurationProperties(Collections.singletonMap(
            "advanced-search-check",
            Collections.singletonMap(
                "existsJavaVersion", "boolean(/project/properties/java.version)"
            ))
        );
        DataSet dataSet = plugin.apply(pluginsConfigurationProperties, createDataSet());
        checkKeyPresent(dataSet, "existsJavaVersion", "true");
    }


    private DataSet createDataSet()
    {
        DataSet dataSet = new DataSet();
        Path repositoryPath = Paths.get("src", "test", "resources");
        dataSet.setAdditionalInformation("repositoryPath", repositoryPath.toFile().getPath());
        return dataSet;
    }


    private PluginsConfigurationProperties createPluginsConfigurationProperties(final Map properties)
    {
        PluginsConfigurationProperties pluginsConfigurationProperties = new PluginsConfigurationProperties();
        pluginsConfigurationProperties.put("maven-report", properties);
        return pluginsConfigurationProperties;
    }


    private void checkKeyPresent(DataSet dataSet, String key, Object expected)
    {
        Optional<Object> response = dataSet.getObjectAdditionalInformation(key);
        assertTrue(response.isPresent());
        assertEquals(expected, response.get());
    }
}