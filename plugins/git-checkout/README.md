## Git Checkout Plugin

### Description

This plugin will clone the source code from the given repository url and checkout to the specific commitId.

### Configuration

This plugin uses the following configuration:

```yaml
sauron.plugins:
    git-checkout:
        publicKey: <git public key>
        privateKey: <git private key>
```

### Input

- sanitizedServiceName: Sanitized service name that is being built, if not present falls back to serviceName.

- repositoryUrl: Repository url that must be cloned.

- commitId: Id of the commit that is being built.

### Output

- repositoryPath: The temporary path where the project has been cloned.