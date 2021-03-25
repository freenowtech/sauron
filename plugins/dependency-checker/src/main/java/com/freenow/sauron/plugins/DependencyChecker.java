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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.cyclonedx.model.Bom;
import org.pf4j.Extension;

@Extension
@Slf4j
public class DependencyChecker implements SauronExtension
{
    DependenciesModel dependenciesModel;

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
                    this.dependenciesModel = new DependenciesModel(input, parseCycloneDx(bom));
                    new ElasticSearchClient(properties).index(dependenciesModel);
                });
        });

        return input;
    }


    private List<Map> parseCycloneDx(Path bom)
    {
        try
        {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            JaxbAnnotationModule module = new JaxbAnnotationModule();
            xmlMapper.registerModule(module);

            ObjectMapper oMapper = new ObjectMapper();
            return Optional.ofNullable(xmlMapper.readValue(bom.toFile(), Bom.class).getComponents()).orElse(Collections.emptyList())
                .stream().map(a -> oMapper.convertValue(a, Map.class)).collect(Collectors.toList());
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }
}