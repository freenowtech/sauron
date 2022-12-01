package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.utils.RetryCommand;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesResources.POD;

@Slf4j
@RequiredArgsConstructor
public class KubernetesEnvironmentVariablesReader
{
    private static final String ENV_COMMAND = "bash -l -c env | grep ^%s";

    private final KubernetesGetObjectMetaCommand kubernetesGetObjectMetaCommand;

    private final KubernetesExecCommand kubernetesExecCommand;


    public KubernetesEnvironmentVariablesReader()
    {
        this.kubernetesGetObjectMetaCommand = new KubernetesGetObjectMetaCommand();
        this.kubernetesExecCommand = new KubernetesExecCommand();
    }


    public void read(DataSet input, String serviceLabel, Collection<String> envVarsCheckProperty, ApiClient apiClient)
    {
        new RetryCommand<Void>().run(() ->
        {
            boolean foundAll = kubernetesGetObjectMetaCommand.get(serviceLabel, POD, input.getServiceName(), apiClient)
                .map(V1ObjectMeta::getName)
                .map(podName -> exec(podName, input, envVarsCheckProperty, apiClient))
                .orElse(false);

            if (!foundAll)
            {
                throw new NoSuchElementException(String.format("Environment variables %s not found.", envVarsCheckProperty));
            }
            return null;
        });
    }


    private Boolean exec(String podName, DataSet input, Collection<String> envVarsCheckProperty, ApiClient apiClient)
    {
        var found = false;
        for (var envVarToCheck : envVarsCheckProperty)
        {
            Optional<String> podEnvVars = kubernetesExecCommand.exec(podName, String.format(ENV_COMMAND, envVarToCheck), apiClient);
            if (podEnvVars.isPresent())
            {
                Properties props = parse(podEnvVars.get());
                if (props.containsKey(envVarToCheck))
                {
                    input.setAdditionalInformation(envVarToCheck, props.get(envVarToCheck));
                    found = true;
                }
                else
                {
                    log.warn(String.format("Environment variable %s, not found in %s", envVarToCheck, props));
                    return false;
                }
            }
            else
            {
                log.warn(String.format("Environment variables %s not found at POD %s", envVarToCheck, podName));
                return false;
            }
        }
        return found;
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
