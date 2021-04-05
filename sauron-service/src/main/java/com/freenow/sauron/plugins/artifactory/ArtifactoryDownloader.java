package com.freenow.sauron.plugins.artifactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jfrog.artifactory.client.Artifactory;
import org.pf4j.update.FileDownloader;

@Slf4j
public class ArtifactoryDownloader implements FileDownloader
{
    private final Artifactory artifactory;


    public ArtifactoryDownloader(Artifactory artifactory)
    {
        this.artifactory = artifactory;
    }


    @Override
    public Path downloadFile(URL fileUrl)
    {
        try
        {
            String path = fileUrl.toString().replace(artifactory.getUri(), "");
            path = path.startsWith("/") ? path.substring(1) : path;
            String repository = path.substring(0, path.indexOf('/'));
            String filePath = path.substring(path.indexOf('/'));
            InputStream inputStream = artifactory.repository(repository).download(filePath).doDownload();
            return saveFile(fileUrl.getPath(), inputStream);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }

        return null;
    }


    private Path saveFile(String path, InputStream is) throws IOException
    {
        Path destination = Files.createTempDirectory("plugins");
        destination.toFile().deleteOnExit();

        String fileName = path.substring(path.lastIndexOf('/') + 1);
        Path tempFile = destination.resolve(fileName);

        try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
            IOUtils.copy(is, out);
        }

        return tempFile;
    }
}
