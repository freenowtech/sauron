## Dependency Checker Plugin

### Description

This plugin analyzes the source code at a given path and generates a comprehensive list of dependencies. The output follows the [CycloneDX](https://cyclonedx.org/) standard.

### Configuration

This plugin does not require mandatory configuration, but specific tool paths can be overridden via `PluginsConfigurationProperties`.

### Input

- `sanitizedServiceName`: Sanitized service name of the project.
- `repositoryPath`: Absolute path to the project's source code.

### Output

- `cycloneDxBomPath`: Path to the generated `bom.xml` (XML) or `bom.json` (JSON).
- `projectType`: Detected project type. Supported types:
   - `MAVEN`
   - `GRADLE_GROOVY`
   - `GRADLE_KOTLIN_DSL`
   - `NODEJS`
   - `PYTHON_REQUIREMENTS`
   - `PYTHON_POETRY`
   - `GO`
   - `SBT`
   - `CLOJURE`
   - `UNKNOWN`

- **Dependency List**: All detected dependencies are output as key-value pairs (Artifact -> Version).
  - To avoid Elasticsearch mapping conflicts, dots (`.`) in artifact IDs are replaced with underscores (`_`).
  - Data is indexed into `dependencies-YYYY` for optimized querying.

### Running locally

This plugin is designed to run in a standalone manner using containerized tools. **No local installation of Python, Node.js, Go, or SBOM tools is required** if you have Docker installed.

#### Docker Image
A pre-configured Docker image providing all necessary runtimes and tools is available. This image is shared with `sauron-service` to ensure consistency.   
Build it using:

```bash
docker build -f ../../sauron-service/Dockerfile --target sauron-tooling -t sauron-tooling:latest ../../sauron-service
```

Once built, the plugin will automatically use this image to execute analysis for `Go`, `NodeJS`, and `Python` projects via the wrapper scripts located in `src/test/resources/bin`.

#### Manual Local Setup (Optional)
If you prefer not to use Docker, you can install the dependencies manually:

##### 1. Go Projects ([Syft](https://github.com/anchore/syft))
```bash
brew tap anchore/syft
brew install syft
```

##### 2. Node.js Projects (NPM)
Requires `npm` (version 9+ recommended for `npm sbom` support).

##### 3. Python Projects
Requires Python 3.11+ and the following tools:

* **Python & pipx**
```bash
brew install python@3.11 
brew install pipx
pipx ensurepath
```

* **Poetry & Export Plugin**
```bash
pipx install poetry
pipx inject poetry poetry-plugin-export
```

* **CycloneDX Python**
```bash
pipx install cyclonedx-bom
```
