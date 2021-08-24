# Cleanup Plugin

## Description

This plugin cleanup Sauron environment used by the pipeline.
The content in `repositoryPath` will be deleted to avoid high usage of storage.

## Configuration

This plugin does not need any configuration.

## Input

- repositoryPath: The temporary path where the project has been cloned.

## Output

This plugin does not add any new keys to final
[DataSet](https://github.com/freenowtech/sauron/blob/main/core/src/main/java/com/freenow/sauron/model/DataSet.java)
object.