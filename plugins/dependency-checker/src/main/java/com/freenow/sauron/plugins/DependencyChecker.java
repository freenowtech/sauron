package com.freenow.sauron.plugins;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.elasticsearch.DependenciesModel;
import com.freenow.sauron.plugins.elasticsearch.ElasticSearchClient;
import com.freenow.sauron.plugins.generator.DependencyGeneratorFactory;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.pf4j.Extension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Extension
@Slf4j
public class DependencyChecker implements SauronExtension
{

    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.getStringAdditionalInformation("repositoryPath").map(Paths::get).ifPresent(repository ->
        {
            ProjectType projectType = ProjectType.fromPath(repository);
            input.setAdditionalInformation("projectType", projectType.toString());

            DependencyGeneratorFactory.newInstance(projectType, properties)
                .map(dependencyGenerator -> dependencyGenerator.generateCycloneDxBom(repository))
                .filter(Files::exists)
                .ifPresent(bom ->
                {
                    input.setAdditionalInformation("cycloneDxBomPath", bom.toString());
                    DependenciesModel dependenciesModel = DependenciesModel.from(input, parseCycloneDx(bom));
                    new ElasticSearchClient(properties).index(dependenciesModel);
                });
        });

        return input;
    }

    private List<Component> parseCycloneDx(Path bom)
    {
        try {
            if (bom.getFileName().toString().endsWith(".xml"))
            {
                return parseCycloneDxXml(bom);
            }
            else if (bom.getFileName().toString().endsWith(".json"))
            {
                return parseCycloneDxJson(bom);
            }
            else
            {
                log.error("Unknown Cyclone DX BOM file format: {}", bom);
            }
        }
        catch (IOException e)
        {
            log.error("Failed to parse Cyclone DX BOM file: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    private List<Component> parseCycloneDxXml(Path bom) throws IOException
    {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JaxbAnnotationModule module = new JaxbAnnotationModule();
        xmlMapper.registerModule(module);

        return Optional.ofNullable(xmlMapper.readValue(bom.toFile(), Bom.class).getComponents()).orElse(Collections.emptyList());
    }


    private List<Component> parseCycloneDxJson(Path bom) throws IOException
    {
        ObjectMapper oMapper = new ObjectMapper();
        var bomContent = oMapper.readValue(bom.toFile(), Bom.class);
        return Optional.ofNullable(bomContent.getComponents()).orElse(Collections.emptyList());
    }
}
