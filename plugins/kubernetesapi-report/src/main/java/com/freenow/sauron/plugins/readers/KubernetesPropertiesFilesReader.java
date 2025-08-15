package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.utils.RetryCommand;
import com.freenow.sauron.plugins.utils.RetryConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesResources.POD;

@Slf4j
@RequiredArgsConstructor
public class KubernetesPropertiesFilesReader
{
    private static final String ENV_COMMAND = "cat %s";
    public static final String KEY_NOT_FOUND_VALUE = "not_found";
    private final KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand;
    private final KubernetesExecCommand kubernetesExecCommand;
    private final RetryConfig retryConfig;


    public KubernetesPropertiesFilesReader()
    {
        this.kubernetesGetObjectMetaCommand = new KubernetesGetObjectMetaCommand();
        this.kubernetesExecCommand = new KubernetesExecCommand();
        this.retryConfig = new RetryConfig();
    }


    public void read(DataSet input, String serviceLabel, Map<String, Map<String, String>> propertiesFilesCheck, ApiClient apiClient)
    {
        new RetryCommand<Void>(retryConfig).run(() ->
        {
            kubernetesGetObjectMetaCommand.get(serviceLabel, POD, input.getServiceName(), apiClient)
                .map(V1ObjectMeta::getName)
                .ifPresent(podName -> exec(podName, input, propertiesFilesCheck, apiClient));
            return null;
        });
    }


    private void exec(final String podName, DataSet input, final Map<String, Map<String, String>> propertiesFilesCheck, final ApiClient apiClient)
    {
        propertiesFilesCheck.forEach((propFilePath, propKeys) -> {
            Optional<String> podFileProps = kubernetesExecCommand.exec(podName, String.format(ENV_COMMAND, propFilePath), apiClient);
            if (podFileProps.isPresent())
            {
                Properties props = parse(podFileProps.get());
                matchProps(input, propFilePath, propKeys, props);
            }
            else
            {
                log.info("Properties file {} not found in POD {}. Properties: {} will be added as not found", propFilePath, podName, String.join(",", propKeys.keySet()));
                propKeys.forEach((key, value) -> {
                    try
                    {
                        input.setAdditionalInformation(key, KEY_NOT_FOUND_VALUE);
                    }
                    catch (NoSuchElementException e)
                    {
                        log.warn("Property Key {} not found at {}", key, propFilePath);
                    }
                });

            }
        });
    }


    private void matchProps(DataSet input, String propFilePath, Map<String, String> propKeys, Properties props)
    {
        propKeys.forEach((key, value) -> {
            if (props.containsKey(value))
            {
                input.setAdditionalInformation(key, props.get(value));
            }
            else
            {
                log.info("Property Key {}, not found at {}", value, propFilePath);
                input.setAdditionalInformation(key, KEY_NOT_FOUND_VALUE);
            }
        });
    }


    private static Properties parse(final String propsStr)
    {
        final Properties props = new Properties();
        try (StringReader sr = new StringReader(propsStr))
        {
            props.load(sr);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
        return props;
    }
}
