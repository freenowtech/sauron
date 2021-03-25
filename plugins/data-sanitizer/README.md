## Data Sanitizer

### Description

This plugin sanitizes the input data in Sauron, removing all non alphanumeric characters and lowering case the
service name and owner.

### Configuration

This plugin does not need a configuration.

### Input

- serviceName: Service name that is being built

- owner: Owner of the service that is being built

### Output

- sanitizedServiceName: Additional information containing the sanitized service name, which means all special characters are removed.

- sanitizedOwner: Additional information containing the sanitized owner, which means all special characters are removed.