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
            boolean foundAll = kubernetesGetObjectMetaCommand.get(serviceLabel, POD, input.getServiceName(), apiClient)
                .map(V1ObjectMeta::getName)
                .map(podName -> exec(podName, input, propertiesFilesCheck, apiClient))
                .orElse(false);

            if (!foundAll)
            {
                throw new NoSuchElementException(String.format("Properties %s not found.", propertiesFilesCheck));
            }
            return null;
        });
    }


    private Boolean exec(final String podName, DataSet input, final Map<String, Map<String, String>> propertiesFilesCheck, final ApiClient apiClient)
    {
        return propertiesFilesCheck.entrySet().stream().allMatch(it -> {
            Optional<String> podFileProps = kubernetesExecCommand.exec(podName, String.format(ENV_COMMAND, it.getKey()), apiClient);
            if (podFileProps.isPresent())
            {
                Properties props = parse(podFileProps.get());
                return matchProps(input, it.getKey(), it.getValue(), props);
            }
            else
            {
                log.info("Properties not found in {} at POD {}", it.getKey(), podName);
                return false;
            }
        });
    }


    private boolean matchProps(DataSet input, String propFilePath, Map<String, String> propKeys, Properties props)
    {
        return propKeys.entrySet().stream().allMatch(prop -> {
            if (props.containsKey(prop.getValue()))
            {
                input.setAdditionalInformation(prop.getKey(), props.get(prop.getValue()));
                return true;
            }
            else
            {
                log.info("Property Key {}, not found at {}", prop.getValue(), propFilePath);
                return false;
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
