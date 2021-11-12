package com.freenow.sauron.plugins.commands;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Streams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.freenow.sauron.plugins.utils.KubernetesConstants.K8S_DEFAULT_NAMESPACE;

@Slf4j
@RequiredArgsConstructor
public class KubernetesExecCommand
{
    private final ApiClient client;


    public Optional<String> exec(String pod, String command)
    {
        String ret = null;
        try
        {
            if (command != null && !command.isEmpty())
            {
                log.debug("Executing command {} in pod {}", command, pod);
                Exec exec = new Exec(client);
                boolean tty = System.console() != null;
                final Process proc = exec.exec(K8S_DEFAULT_NAMESPACE, pod, command.split(" "), true, tty);

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Thread out = newThreadStream(proc.getInputStream(), output);

                ByteArrayOutputStream error = new ByteArrayOutputStream();
                Thread err = newThreadStream(proc.getErrorStream(), error);

                proc.waitFor();

                out.join();
                err.join();

                proc.destroy();

                log.debug("Command {} in pod {} returned {}", command, pod, proc.exitValue());
                if (proc.exitValue() != 0)
                {
                    log.error("Error executing the command '{}' in pod '{}', Error: {}", command, pod, error);
                }
                else
                {
                    ret = output.toString();
                }
            }
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }

        return Optional.ofNullable(ret);
    }


    private Thread newThreadStream(InputStream in, OutputStream out)
    {
        Thread thread = new Thread(() ->
        {
            try
            {
                Streams.copy(in, out);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        });
        thread.start();

        return thread;
    }
}
