## Readme Checker Plugin

### Description

Readme Checker is responsible for check whether a service has or not a README.md file in its root.

### Configuration

This plugin uses the following configuration:

```yaml
sauron.plugins:
    readme-checker:
        minLength: <minimum readme length e.g. '1B', '10KB' - default value 1B>
        caseSensitive: <if true only 'README.md' is a valid name - default value false>
```

### Input

- repositoryPath: Path to the source code project

### Output

- missingOrEmptyReadme: Boolean that describes whether a project has or not the README.md file.
