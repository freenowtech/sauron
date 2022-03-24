package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.plugins.commands.CloneCommandFactory;
import com.freenow.sauron.properties.PluginsConfigurationProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.pf4j.Extension;

@Extension
@Slf4j
public class GitCheckout implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        try
        {
            // Checkout the repository.
            String serviceName = input.getStringAdditionalInformation("sanitizedServiceName").orElse(input.getServiceName());
            Path destination = checkout(input.getRepositoryUrl(), input.getCommitId(), serviceName, properties);
            input.setAdditionalInformation("repositoryPath", destination.toFile().getPath());

            // Checkout the custom repository.
            final String customRepositoryUrl = input.getStringAdditionalInformation("customizedRepositoryUrl").orElse(null);
            if (customRepositoryUrl != null && !customRepositoryUrl.isBlank() && !input.getRepositoryUrl().equalsIgnoreCase(customRepositoryUrl))
            {
                destination = checkout(customRepositoryUrl, "master", "custom-" + serviceName, properties);
            }
            input.setAdditionalInformation("customizedRepositoryPath", destination.toFile().getPath());

        } catch (GitAPIException | IOException e)
        {
            log.error(e.getMessage(), e);
        }

        return input;
    }


    /**
     * Checkout repository
     *
     * @param repositoryUrl         The repository Url.
     * @param checkoutName          The branch / commit to checkout.
     * @param checkoutDirectoryName The local directory name to make the checkout.
     * @param properties            The plugins configuration properties.
     * @return The destination path of the checked out repository.
     * @throws IOException     in case of any input/output exception.
     * @throws GitAPIException in case of any Git API exception.
     */
    private Path checkout(final String repositoryUrl, final String checkoutName, final String checkoutDirectoryName, final PluginsConfigurationProperties properties)
        throws IOException, GitAPIException
    {
        Path destination = Files.createTempDirectory(checkoutDirectoryName);
        destination.toFile().deleteOnExit();

        CloneCommand command = CloneCommandFactory.instance(repositoryUrl, destination, properties);
        command.call().checkout().setName(checkoutName).call();
        return destination;
    }

}