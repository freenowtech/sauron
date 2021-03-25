package com.freenow.sauron.plugins.generator;

import java.nio.file.Path;

public interface DependencyGenerator
{
    Path generateCycloneDxBom(Path repositoryPath);
}
