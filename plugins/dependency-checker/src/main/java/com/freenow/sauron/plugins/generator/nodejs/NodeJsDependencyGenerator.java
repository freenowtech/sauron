package com.freenow.sauron.plugins.generator.nodejs;

import com.freenow.sauron.plugins.generator.DependencyGenerator;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public abstract class NodeJsDependencyGenerator extends DependencyGenerator {
    private final String lockfile;

    protected NodeJsDependencyGenerator(PluginsConfigurationProperties properties, String lockfile) {
        super(properties);
        this.lockfile = lockfile;
    }

    private static class PackageJsonMissingException extends IllegalStateException {
        private PackageJsonMissingException() {
            super("Project is missing package.json");
        }
    }

    private static class LockfileNotFoundException extends IllegalStateException {
        private LockfileNotFoundException(String lockfile) {
            super("Lockfile " + lockfile + " was not found.");
        }
    }

    @Override
    public Path generateCycloneDxBom(Path repositoryPath) {
        try
        {
            requirePackageJson(repositoryPath);
            return buildCycloneDxBom(repositoryPath);
        }
        catch (IllegalStateException e)
        {
            log.info("Skip building Cyclone DX BOM: {}", e.getMessage());
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    protected abstract Path buildCycloneDxBom(Path repositoryPath) throws IOException, InterruptedException;

    private void requirePackageJson(Path repositoryPath)
    {
        if (Files.notExists(repositoryPath.resolve("package.json")))
        {
            throw new PackageJsonMissingException();
        }
        if (Files.notExists(repositoryPath.resolve(this.lockfile)))
        {
            throw new LockfileNotFoundException(this.lockfile);
        }
    }
}
