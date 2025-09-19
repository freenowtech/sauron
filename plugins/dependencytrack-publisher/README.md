## Dependencytrack Publisher Plugin

### Description

This plugin takes the generated SBOM file from the previous step and publishes it to our internal
[Dependency Track](https://dependencytrack.org/) instance.

### Configuration

This plugin uses the following configuration:

```yaml
sauron.plugins:
    dependencytrack-publisher:
        uri: <dependencytrack uri>
        api-key: <dependencytrack api-key>
        environments:
            - "live"
```

### Input

- sanitizedServiceName: Sanitized service name that is being built, if not present falls back to serviceName.

- release: Release identifier. In case it is not provided, uses commitId.

- commitId: Id of the commit that is being built.

- cycloneDxBomPath: Path to the `bom.xml` or `bom.json` file.

### Output

This plugin does not produce output.
