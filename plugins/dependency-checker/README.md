## Dependency Checker Plugin

### Description

This plugin will checkout the code for the commitId provided in build, and generate all dependencies for that specific
project. The output will be a a list of objects that describes the dependencies.The output format follows the standard
[CycloneDX](https://cyclonedx.org/#specification-overview).

### Configuration

This plugin does not need a configuration.

### Input

- sanitizedServiceName: Sanitized service name that is being built.

- repositoryPath: Path to the source code project.

### Output

- cycloneDxBomPath: Path to the cycloneDx `bom.xml`

- projectType: Project type. Possible values:
   - `MAVEN`
   - `GRADLE_GROOVY`
   - `GRADLE_KOTLIN_DSL`
   - `NODEJS`
   - `UNKNONW`

- dependency key/value list: This plugin outputs all dependencies as a key and its version as a value.
This is done to achieve the use case where we need to filter the documents by the version of a specific
dependency. The list of dependencies is stored in a different index pattern `dependencies-YYYY` for 
performance improvement.

*Note*: Dependencies with `.` in artifact id, will have this character replaced by `_` to avoid mapping conflicts
in Elasticsearch. See this [issue](https://github.com/elastic/kibana/issues/3540#issuecomment-219808228) for more details.


### Running locally

Tu run this plugin locally the below dependencies are required:
* Python
```bash
brew install python@3.11.4 
``` 
* Poetry
```bash
brew python -m pip install poetry==1.1.7
```