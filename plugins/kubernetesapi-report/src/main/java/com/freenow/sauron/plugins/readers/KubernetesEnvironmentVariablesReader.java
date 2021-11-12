package com.freenow.sauron.plugins.readers;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.KubernetesExecCommand;
import com.freenow.sauron.plugins.commands.KubernetesGetObjectMetaCommand;
import io.kubernetes.client.openapi.ApiClient;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesResources.POD;

@Slf4j
@RequiredArgsConstructor
public class KubernetesEnvironmentVariablesReader
{
    private static final String ENV_COMMAND = "env";

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
            .flatMap(objectMeta -> kubernetesExecCommand.exec(objectMeta.getName(), new String[] {ENV_COMMAND}))
            .flatMap(KubernetesEnvironmentVariablesReader::parse)
            .ifPresent(envVars ->
                envVarsCheckProperty.forEach(check ->
                {
                    if (envVars.containsKey(check))
                    {
                        input.setAdditionalInformation(check, envVars.get(check));
                    }
                })
            );
    }


    private static Optional<Properties> parse(final String propsStr)
    {
        final Properties props = new Properties();
        try (StringReader sr = new StringReader(propsStr))
        {
            props.load(sr);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }

        return Optional.of(props);
    }
}
