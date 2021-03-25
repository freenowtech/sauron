## Maven Report Plugin

### Description

This plugin creates named checkers looking at service's direct dependencies where written in the service's POM
 file. Therefore, the dependencies from the parent POM are not included. 

### Configuration

- direct-dependency-check: Map of all dependencies to be checked whether it is a direct dependency or not. 
    - Key: the name of the property to be added to the DataSet. 
    - value: The dependency in this format "groupId:artifactIdPrefix[:scope]" (e.g. "com.freenow:service:compile").
- advanced-search-check: Map of all keys and a respective [XPath](https://www.w3schools.com/xml/xpath_intro.asp
) expression that will be evaluated, and the return value will be set in Dataset using the given key.
[XPathGenerator](https://xmltoolbox.appspot.com/xpath_generator.html) can be used to ease the expression creation.
    - Key: the name of the property to be added to the DataSet.
    - value: The XPath expression to be evaluated

### Input

- repositoryPath: The repository path of the project.

### Output

- All keys in the `direct-dependency-check` map will be added to the `DataSet` with either `true` or `false` to
 indicate whether dependency exists.
 
- All the keys in `advanced-search-check` map will be added to `Dataset` after evaluate the respective XPath expression
