package com.freenow.sauron.plugins.elasticsearch;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.NormalizeDependencyVersion;
import com.freenow.sauron.plugins.ProjectType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.LicenseChoice;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;

@Slf4j
@Data
public class DependenciesModel
{
    private final String serviceName;
    private final String commitId;
    private final Date eventTime;
    private final String buildId;
    private final String owner;
    private final String environment;
    private final String projectType;
    private final Map<String, Dependency> dependencies;

    @JsonAnyGetter
    public Map<String, Object> getDependencies() {
        Map<String, Object> result = new HashMap<>();

        Set<License> licenses = new HashSet<>();

        for (Map.Entry<String, Dependency> dependencyEntry : dependencies.entrySet()) {
            String key = dependencyEntry.getKey();
            Dependency dependency = dependencyEntry.getValue();

            result.put(key, dependency.version);
            result.put(key.concat("-normalized"), dependency.normalizedVersion);
            result.put(key.concat("-license"), dependency.license);

            licenses.addAll(dependency.licenses);
        }

        result.put("licenses", licenses);

        return result;
    }

    @Data
    private static class Dependency
    {
        @JsonIgnore
        private final String name;
        private final String version;
        private final String normalizedVersion;
        private final String license;
        private final List<License> licenses;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    private static class License
    {
        private final String id;
        private final String name;
        private final String url;

        private static List<License> createLicenseList(LicenseChoice licenseChoice)
        {
            if (licenseChoice == null)
            {
                return Collections.emptyList();
            }
            if (licenseChoice.getLicenses() == null)
            {
                return Collections.emptyList();
            }

            return licenseChoice.getLicenses().stream().map(license -> new License(license.getId(), license.getName(), license.getUrl())).collect(Collectors.toList());
        }
    }


    public static DependenciesModel from(DataSet dataSet, List<Component> dependencies)
    {
        ProjectType projectType = dataSet.getStringAdditionalInformation("projectType")
            .map(ProjectType::valueOf)
            .orElse(ProjectType.UNKNOWN);

        return new DependenciesModel(
            dataSet.getServiceName(),
            dataSet.getCommitId(),
            dataSet.getEventTime(),
            dataSet.getBuildId(),
            dataSet.getOwner(),
            dataSet.getStringAdditionalInformation("environment").orElse("none"),
            projectType.name(),
            dependencies.stream().collect(Collectors.toMap(
                dependency -> determineKey(projectType, dependency),
                dependency -> {
                    String version = String.valueOf(dependency.getVersion());
                    String normalizedVersion = NormalizeDependencyVersion.toMajorMinorIncremental(version);
                    List<License> licenses = License.createLicenseList(dependency.getLicenseChoice());
                    String license = licenses.stream().findFirst().flatMap(l -> Optional.ofNullable(l.getId())).orElse(EMPTY_STRING);

                    return new Dependency(
                        determineKey(projectType, dependency),
                        version,
                        normalizedVersion,
                        license,
                        licenses
                    );
                },
                (dependency1, dependency2) -> {
                    if (!Objects.equals(dependency1, dependency2)) {
                        log.warn("Inconsistent duplicated dependency found: {}, {}", dependency1, dependency2);
                    }
                    return dependency1;
                }
            ))
        );
    }


    private static String determineKey(ProjectType projectType, Component dependency)
    {
        String group = dependency.getGroup();
        String name = dependency.getName();

        /*
         * Node and python modules don't have a group but can have a scope (e.g. @<scope>/package-name) which CycloneDX
         * uses as group replacement. To avoid conflicts between node modules and dependencies from other
         * ecosystems all node modules are assigned the group default group according to the project type.
         */
        if (projectType.hasNullGroup())
        {
            if (group != null)
            {
                name = String.format("%s/%s", group, name);
            }

            group = projectType.defaultGroup();
        }

        return String.format("%s:%s", group, name.replace(".", "_"));
    }


    public String toJson() throws JsonProcessingException
    {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        return jsonMapper.writeValueAsString(this);
    }
}
