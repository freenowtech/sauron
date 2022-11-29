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
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesResources.POD;

@Slf4j
@RequiredArgsConstructor
public class KubernetesEnvironmentVariablesReader
{
    private static final String ENV_COMMAND = "bash -l -c env";

    private final KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand;

    private final KubernetesExecCommand kubernetesExecCommand;

    private final RetryConfig retryConfig;


    public KubernetesEnvironmentVariablesReader()
    {
        this.kubernetesGetObjectMetaCommand = new KubernetesGetObjectMetaCommand();
        this.kubernetesExecCommand = new KubernetesExecCommand();
        this.retryConfig = new RetryConfig();
    }


    public void read(DataSet input, String serviceLabel, Collection<String> envVarsCheckProperty, ApiClient apiClient)
    {
        new RetryCommand<Void>(retryConfig).run(() ->
        {
            boolean foundAll = kubernetesGetObjectMetaCommand.get(serviceLabel, POD, input.getServiceName(), apiClient)
                .map(V1ObjectMeta::getName)
                .map(podName -> exec(podName, input, envVarsCheckProperty, apiClient))
                .orElse(false);

            if (!foundAll)
            {
                throw new NoSuchElementException("Not all Environment variables could be found.");
            }
            return null;
        });
    }


    private Boolean exec(String podName, DataSet input, Collection<String> envVarsCheckProperty, ApiClient apiClient)
    {
        return kubernetesExecCommand.exec(podName, ENV_COMMAND, apiClient).map(ret ->
        {
            Properties envVars = parse(ret);
            return envVarsCheckProperty.stream().allMatch(check ->
            {
                if (envVars.containsKey(check))
                {
                    input.setAdditionalInformation(check, envVars.get(check));
                    return true;
                }
                else
                {
                    log.warn(String.format("Environment variable %s could not be found", check));
                    return false;
                }
            });
        }).orElse(false);
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
