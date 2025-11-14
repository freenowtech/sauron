package com.freenow.sauron.plugins;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.UUID;

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
        JsonNode bomNode = oMapper.readTree(bom.toFile());

        /*
         * The npm BOM generator may produce an invalid serialNumber, which can cause validation issues in DependencyTrack.
         * https://github.com/DependencyTrack/dependency-track/blob/fa1eb0bb4c1ecf87d231a21e077055acb6b8b59d/src/main/java/org/dependencytrack/parser/cyclonedx/CycloneDxValidator.java#L90
         * which returns error like this
         * {"status":400,"title":"The uploaded BOM is invalid","detail":"Schema validation failed","errors":["$.serialNumber: does not match the regex pattern ^urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"]}
         * This code replaces the invalid serialNumber with a valid v4 UUID to ensure compatibility.
         */
        if (bomNode.has("serialNumber") && bomNode.get("serialNumber").asText().contains("***"))
        {
            log.debug("Replacing invalid serialNumber in {} for project: {}", bom.getFileName(), bom.getParent());
            ((ObjectNode) bomNode).put("serialNumber", "urn:uuid:" + UUID.randomUUID());
        }

        Bom bomObject = oMapper.treeToValue(bomNode, Bom.class);

        return Optional.ofNullable(bomObject.getComponents()).orElse(Collections.emptyList());
    }
}
