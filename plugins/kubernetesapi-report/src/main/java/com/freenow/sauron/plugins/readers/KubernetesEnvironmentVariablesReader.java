package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import com.freenow.sauron.plugins.utils.RetryCommand;
import io.kubernetes.client.openapi.ApiClient;
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


    public KubernetesEnvironmentVariablesReader(ApiClient client)
    {
        this.kubernetesGetObjectMetaCommand = new KubernetesGetObjectMetaCommand(client);
        this.kubernetesExecCommand = new KubernetesExecCommand(client);
    }


    public void read(DataSet input, String serviceLabel, Collection<String> envVarsCheckProperty)
    {
        kubernetesGetObjectMetaCommand.get(serviceLabel, POD, input.getServiceName())
            .ifPresent(objectMeta -> new RetryCommand<Void>().run(() -> exec(objectMeta.getName(), input, envVarsCheckProperty)));
    }


    private Void exec(String name, DataSet input, Collection<String> envVarsCheckProperty)
    {
        boolean foundAll = kubernetesExecCommand.exec(name, ENV_COMMAND).map(ret ->
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
        }).orElseThrow();

        if (!foundAll)
        {
            throw new NoSuchElementException("Not all Environment variables could be found.");
        }

        return null;
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
