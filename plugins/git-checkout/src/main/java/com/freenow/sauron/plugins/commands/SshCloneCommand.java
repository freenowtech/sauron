package com.freenow.sauron.plugins.commands;

import com.freenow.sauron.properties.PluginsConfigurationProperties;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;

import static com.freenow.sauron.plugins.utils.CloneCommandHelper.getStringProperty;

@Slf4j
public final class SshCloneCommand extends CloneCommand
{
    SshCloneCommand(String repositoryUrl, Path destination, PluginsConfigurationProperties properties)
    {
        this.setURI(repositoryUrl).setDirectory(destination.toFile());
        this.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(getSshSessionFactory(properties));
        });
    }


    private SshSessionFactory getSshSessionFactory(PluginsConfigurationProperties properties)
    {
        return new JschConfigSessionFactory()
        {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session)
            {
                getStringProperty("password", properties).ifPresent(session::setPassword);
                session.setConfig("StrictHostKeyChecking", "no");
            }


            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException
            {
                JSch defaultJSch = super.createDefaultJSch(fs);
                Optional<String> privateKeyPath = getKeyPath(properties, "privateKey", "id_rsa");
                Optional<String> publicKeyPath = getKeyPath(properties, "publicKey", "id_rsa.pub");
                if (privateKeyPath.isPresent() && publicKeyPath.isPresent())
                {
                    defaultJSch.addIdentity(privateKeyPath.get(), publicKeyPath.get(), null);
                }
                return defaultJSch;
            }
        };
    }


    private Optional<String> getKeyPath(PluginsConfigurationProperties properties, String keyName, String keyFile)
    {
        return properties.getPluginConfigurationProperty("git-checkout", keyName)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(key -> createNewFile(keyFile, key));
    }


    private String createNewFile(String filename, String content)
    {
        File privateKey = new File(filename);
        try
        {
            if (privateKey.createNewFile())
            {
                try (FileOutputStream oFile = new FileOutputStream(privateKey, false))
                {
                    oFile.write(content.getBytes());
                }
                finally
                {
                    privateKey.deleteOnExit();
                }
            }
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
        return privateKey.getPath();
    }
}