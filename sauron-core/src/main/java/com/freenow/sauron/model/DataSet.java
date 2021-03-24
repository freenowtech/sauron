package com.freenow.sauron.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSet
{
    private String serviceName;

    private String repositoryUrl;

    private String commitId;

    private String buildId;

    private String owner;

    private Date eventTime;

    @Builder.Default
    private Map<String, Object> additionalInformation = new HashMap<>();


    public Optional<String> getStringAdditionalInformation(String key)
    {
        return getAdditionalInformation(key, String.class);
    }


    public Optional<Long> getLongAdditionalInformation(String key)
    {
        return getAdditionalInformation(key, Long.class);
    }


    public Optional<Integer> getIntegerAdditionalInformation(String key)
    {
        return getAdditionalInformation(key, Integer.class);
    }


    public Optional<Double> getDoubleAdditionalInformation(String key)
    {
        return getAdditionalInformation(key, Double.class);
    }


    public Optional<Boolean> getBooleanAdditionalInformation(String key)
    {
        return getAdditionalInformation(key, Boolean.class);
    }


    public Optional<Object> getObjectAdditionalInformation(String key)
    {
        return getAdditionalInformation(key, Object.class);
    }


    @JsonAnySetter
    public DataSet setAdditionalInformation(String key, Object value)
    {
        this.additionalInformation.put(key, value);
        return this;
    }


    @JsonAnyGetter
    private Map<String, Object> getAdditionalInformation()
    {
        return additionalInformation;
    }


    public Map<String, Object> copyAdditionalInformation()
    {
        return new HashMap<>(additionalInformation);
    }


    public String toJson() throws JsonProcessingException
    {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        return jsonMapper.writeValueAsString(this);
    }


    private <T> Optional<T> getAdditionalInformation(String key, Class<T> type)
    {
        Object value = additionalInformation.get(key);
        return Optional.ofNullable((type.isInstance(value)) ? type.cast(value) : null);
    }
}