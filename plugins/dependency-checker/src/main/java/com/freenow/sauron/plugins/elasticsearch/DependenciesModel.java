package com.freenow.sauron.plugins.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.NormalizeDependencyVersion;
import com.freenow.sauron.plugins.ProjectType;

import java.util.*;

import static ch.qos.logback.core.CoreConstants.EMPTY_STRING;

public class DependenciesModel extends HashMap<String, Object>
{
    private static final String LICENSES = "licenses";

    public DependenciesModel(DataSet dataSet, List<Map> dependencies)
    {
        this.put("serviceName", dataSet.getServiceName());
        this.put("commitId", dataSet.getCommitId());
        this.put("eventTime", dataSet.getEventTime());
        this.put("buildId", dataSet.getBuildId());
        this.put("owner", dataSet.getOwner());

        this.put("environment", dataSet.getStringAdditionalInformation("environment").orElse("none"));

        String projectType = dataSet.getStringAdditionalInformation("projectType").orElse("none");
        this.put("projectType", projectType);

        dependencies.forEach(dependency ->
        {
            String key = determineKey(ProjectType.valueOf(projectType), dependency);
            String value = String.valueOf(dependency.get("version"));
            this.put(key, value);
            this.put(key.concat("-normalized"), NormalizeDependencyVersion.toMajorMinorIncremental(value));

            List<Map> mapOfLicenses = (List<Map>) Optional.ofNullable(
                    ((LinkedHashMap) Optional.ofNullable(dependency.getOrDefault(LICENSES, new LinkedHashMap<>())).orElse(new LinkedHashMap<>())).getOrDefault(LICENSES, Collections.emptyList())
            ).orElse(Collections.emptyList());

            String licenseId = mapOfLicenses.stream()
                    .findFirst()
                    .map(license -> license.getOrDefault("id", EMPTY_STRING))
                    .orElse(EMPTY_STRING)
                    .toString();

            this.put(key.concat("-license"), licenseId);

            Set<Map> allLicenses = (Set<Map>) this.getOrDefault(LICENSES, new HashSet<>());
            allLicenses.addAll(mapOfLicenses);
            this.put(LICENSES, allLicenses);
        });
    }


    private String determineKey(ProjectType projectType, Map<?, ?> dependency)
    {
        String group = (String) dependency.get("group");
        String name = (String) dependency.get("name");

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
