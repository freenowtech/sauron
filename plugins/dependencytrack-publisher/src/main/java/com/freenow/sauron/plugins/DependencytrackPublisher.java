package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Extension
@Slf4j
public class DependencytrackPublisher implements SauronExtension
{
    private static final String PLUGIN_ID = "dependencytrack-publisher";

    private RestTemplate restTemplate;


    public DependencytrackPublisher()
    {
        this.restTemplate = new RestTemplate();
    }


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        Optional<String> environment = input.getStringAdditionalInformation("environment");
        Collection environments = properties.getPluginConfigurationProperty(PLUGIN_ID, "environments")
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(Map::values)
            .orElse(Collections.emptyList());


        if (environments.isEmpty() || environment.isEmpty() || environment.get().isBlank() || environments.contains(environment.get()))
        {
            properties.getPluginConfigurationProperty(PLUGIN_ID, "uri").ifPresent(uri ->
                properties.getPluginConfigurationProperty(PLUGIN_ID, "api-key").ifPresent(apiKey ->
                    input.getStringAdditionalInformation("cycloneDxBomPath").flatMap(this::encodeFileToBase64).ifPresent(encodedFile ->
                        {
                            try
                            {
                                HttpHeaders headers = new HttpHeaders();
                                headers.set("X-Api-Key", (String) apiKey);
                                headers.add("Accept-Encoding", "gzip");
                                headers.add("Accept-Encoding", "deflate");
                                headers.add("Accept-Encoding", "br");

                                Map<String, Object> param = new HashMap<>(4);
                                param.put("projectName", input.getServiceName());
                                param.put("projectVersion", environment.orElse(input.getStringAdditionalInformation("release").orElse(input.getCommitId())));
                                param.put("autoCreate", true);
                                param.put("bom", encodedFile);

                                HttpEntity<?> requestEntity = new HttpEntity<>(param, headers);
                                restTemplate.put(new URI(uri + "/api/v1/bom"), requestEntity);
                            }
                            catch (URISyntaxException e)
                            {
                                log.error(e.getMessage(), e);
                            }
                        }
                    )
                )
            );
        }

        return input;
    }


    private Optional<String> encodeFileToBase64(String filename)
    {
        File file = new File(filename);
        if (file.exists())
        {
            try
            {
                byte[] encoded = Base64.getEncoder().encode(Files.readAllBytes(file.toPath()));
                return Optional.of(new String(encoded, StandardCharsets.US_ASCII));
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
            }
        }

        return Optional.empty();
    }
}