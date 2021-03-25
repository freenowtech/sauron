package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.StringUtils;
import org.pf4j.Extension;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Slf4j
@Extension
public class MavenReport implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.getStringAdditionalInformation("repositoryPath").ifPresent(repositoryPath ->
        {
            File pom = new File(String.format("%s/pom.xml", repositoryPath));
            if(pom.exists())
            {
                try
                {
                    final Model directPomModel = new MavenXpp3Reader().read(new FileReader(pom));
                    setDirectDependenciesInformation(properties, input, directPomModel.getDependencies());
                    setAdvancedSearchInformation(properties, input, pom);
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                }
            }
        });
        return input;
    }


    private void setDirectDependenciesInformation(final PluginsConfigurationProperties properties, final DataSet input, final List<Dependency> directDependencies)
    {
        properties.getPluginConfigurationProperty("maven-report", "direct-dependency-check").ifPresent(directDependenciesCheck ->
        {
            Map<String, String> checkDependencyProperty = (Map<String, String>) directDependenciesCheck;
            checkDependencyProperty.forEach((key, value) ->
            {
                final String[] dependencyProperties = value.split(":");
                if (dependencyProperties.length == 2 || dependencyProperties.length == 3)
                {
                    Dependency dependency = new Dependency();
                    dependency.setGroupId(dependencyProperties[0]);
                    dependency.setArtifactId(dependencyProperties[1]);
                    if (dependencyProperties.length == 3)
                    {
                        dependency.setScope(dependencyProperties[2]);
                    }
                    input.setAdditionalInformation(key, hasDependency(directDependencies, dependency));
                }
                else
                {
                    log.error("Wrong dependency format. Please use \"groupId:artifactIdPrefix[:scope]\" (e.g. \"com.freenow:service:compile\"");
                }
            });
        });
    }


    private void setAdvancedSearchInformation(PluginsConfigurationProperties properties, DataSet input, File pomFile)
    {
        properties.getPluginConfigurationProperty("maven-report", "advanced-search-check").ifPresent(advancedSearchCheck ->
        {
            try
            {
                Map<String, String> advancedSearchProperty = (Map) advancedSearchCheck;
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document xmlDocument = builder.parse(pomFile);
                advancedSearchProperty.forEach((key, value) ->
                {
                    try
                    {
                        XPath xpath = XPathFactory.newInstance().newXPath();
                        input.setAdditionalInformation(key, xpath.compile(value).evaluate(xmlDocument));
                    }
                    catch (XPathExpressionException e)
                    {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            catch (ParserConfigurationException | SAXException | IOException e)
            {
                log.error(e.getMessage(), e);
            }
        });
    }


    private Boolean hasDependency(final List<Dependency> dependencies, final Dependency dependency)
    {
        return getDependency(dependencies, dependency).isPresent();
    }


    private Optional<Dependency> getDependency(final List<Dependency> dependencies, Dependency dependency)
    {
        return dependencies.stream().filter(pomDependency ->
            pomDependency.getGroupId().equals(dependency.getGroupId()) && pomDependency.getArtifactId().contains(dependency.getArtifactId()) && (StringUtils.isBlank(
                dependency.getScope()) || (StringUtils.isNotBlank(pomDependency.getScope()) && pomDependency.getScope().equalsIgnoreCase(dependency.getScope())))).findFirst();
    }
}