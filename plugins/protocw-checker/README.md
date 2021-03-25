## Protoc Wrapper Checker Plugin

### Description

Some of our microservices use _protobuf_  as means of communication.

We would need to have a compiler for protobuf installed in our computers, and provide a reference in the pom.xml to the path where the compiler was installed.

To cope with that it is possible to include a _protoc_ wrapper in the process, in a similar to what _mvnw_ and _gradlew_ wrapper do. 


### Configuration

This plugin uses the following configuration:

```yaml
sauron.plugins:
    protocw-checker:
        protocw-file-name: [name of the protoc wrapper : protocw]
        protocw-properties-file-name: [name of the protoc properties file : protocw.properties]
```

### What to do when you don't have protoc wrapper
[Add it](https://github.com/freenowtech/protoc-wrapper)

### Input

- `repositoryPath`: Path to the source code project

### Output

- `missingProtocw`: Boolean that describes whether a project has or not the protocw.
- `protocVersion`: SemVer protoc version