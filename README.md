# SAURON - VERSION AND DEPLOYMENT TRACKER

[![Release](https://img.shields.io/github/v/release/freenowtech/sauron)](https://github.com/freenowtech/sauron/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/freenowtech/sauron/total?label=release%20binary%20downloads)](https://github.com/freenowtech/sauron/releases/latest)
[![Build](https://github.com/freenowtech/sauron/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/freenowtech/sauron/actions/workflows/build.yml)
[![Release Components](https://github.com/freenowtech/sauron/actions/workflows/release-components.yml/badge.svg)](https://github.com/freenowtech/sauron/actions/workflows/release-components.yml)
[![Release Plugins](https://github.com/freenowtech/sauron/actions/workflows/release-plugins.yml/badge.svg)](https://github.com/freenowtech/sauron/actions/workflows/release-plugins.yml)

<p align="center" style="background-color:black;">
  <img src="https://steamuserimages-a.akamaihd.net/ugc/541883619699450274/D25F66426956C58E110013352AE49102BD01BCE2/" />
</p>

## DESCRIPTION

Sauron, the all seeing eye! It is a service to generate automated reports and track migrations, changes and dependency
versions for backend services also report on known CVE and security issues. A detailed description can be
found in the internal RFC document.

---

## COMPONENTS

Sauron Service is segregated into a few components that are described below:

- **Sauron Core:** Sauron common library to be used by all sauron plugins. More details
[here](https://github.com/freenowtech/sauron/tree/main/sauron-core)

- **Sauron Service:** Sauron entrypoint controller described in more details in this README

- **Sauron Plugin System:** Sauron has an embedded plugin support that allows anyone to introduce its own logic without
the need to rebuild, regenerate and stop/restart Sauron Service. More details in further sections.

- **Elasticsearch Cluster:** Sauron uses [Elasticsearch](https://www.elastic.co) as a data storage. It is currently
deployed in AWS and can be accessed
[here](http://localhost:9200).

- **Kibana:** Elasticsearch data can be explored using the built-in [Kibana](https://www.elastic.co/products/kibana)
instance that can be accessed [here](http://localhost:5601).

- **Dependencytrack:** Sauron includes an instance of [Dependencytrack](https://dependencytrack.org), a platform that
 allows organizations to identify and reduce risk from the use of third-party and open source components.

---

## ARCHITECTURE OVERVIEW

![Sauron Service Architecture](https://github.com/freenowtech/sauron/blob/main/sauron-service/docs/sauron-service.png)

---

## RUNNING

Sauron can be deployed using [Docker](https://www.docker.com/) and [Docker-Compose](https://docs.docker.com/compose/).
It provides a Dockerfile that can be built using the following commands:

Build Sauron project using maven and ship it to a docker image:

```commandline
make
```

Start Sauron stack and load the [local plugin repository](https://github.com/freenowtech/sauron/blob/main/sauron-service/plugins/plugins.json)

```commandline
docker-compose -f docker-compose.yml --compatibility up
```

This command will start three components:

- elasticsearch: http://localhost:9200
- kibana: http://localhost:5601
- sauron-service: http://localhost:8080

**Note**: *Since Sauron needs your maven, gradle and nodejs configuration, the `docker-compose.yml` creates a volume
for each configuration folder/file. If there is no configuration already created, please create one before running
the command above. For more details please refer to
[docker-compose.yaml > Volumes](https://github.com/freenowtech/sauron/blob/main/sauron-service/docker-compose.yml?at=master#48)*

### Docker

In order to run Sauron with you predefined configuration using docker, use the command below:

```shell
docker run \
    -e SPRING_CONFIG_LOCATION="/sauron/config/sauron-service.yml" \
    -e M2_HOME="/usr/share/maven" \
    -e SPRING_PROFILES_INCLUDE="local" \
    -e SPRING_CLOUD_CONFIG_ENABLED="false" \
    --mount type=bind,source=${PWD}/sauron-service/docker/config/sauron-service.yml,destination=/sauron/config/sauron-service.yml,readonly \
    --mount type=bind,source=${PWD}/sauron-service/plugins,destination=/sauron/plugins \
    --mount type=bind,source=${HOME}/.m2,destination=/root/.m2 \
    --mount type=bind,source=${HOME}/.gradle,destination=/root/.gradle \
    --mount type=bind,source=${HOME}/.npmrc,destination=/root/.npmrc \
    --mount type=bind,source=${HOME}/.ssh,destination=/root/.ssh,readonly \
    --name=sauron \
    -p 8080:8080 \
    ghcr.io/freenowtech/sauron/sauron-service:latest
```

If you need to use a specific version, please refer to [Sauron Packages](https://github.com/orgs/freenowtech/packages/container/package/sauron%2Fsauron-service)

---

## CONFIGURATION

Sauron configuration can be set via `application.properties` file. The file path can be provided using the environment
variable:

- **SPRING_CONFIG_LOCATION:** it must contain the path pointing to the local file
e.g. `/path/to/config/my-properties.properties` or `/path/to/config/my-properties.yaml`

Sauron supports
[Spring Cloud Config Server](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html)
as a configuration provider. In order to set Config Server url, please use the environment variable below:

- **SPRING_CLOUD_CONFIG_URI:** it must contain the URL pointing to the remote repository
e.g. `https://my-repository.com/my-config`

For a Sauron configuration file example, please refer to
[sauron-service.yml](https://github.com/freenowtech/sauron/blob/main/sauron-service/docker/config/sauron-service.yml)

---

## USAGE

### Sauron Elasticsearch Index Template

Before start using Sauron, it is import to define the
[index template](https://www.elastic.co/guide/en/elasticsearch/reference/6.8/indices-templates.html) that will be used
by Elasticsearch to create Sauron's index. It can be done by running the command line below:

 ```commandline
elasticsearch/sauron-template.sh
elasticsearch/dependencies-template.sh
```

This template increases the number of fields, since usually the amount of dependencies and thus the amount of fields is
huge, and some other minor optimizations.

### Triggering Sauron Service

Sauron Service provides a REST api that allows one to trigger a new build. The detailed parameters can be found in its
[swagger documentation](http://localhost:8080/v2/api-docs). Once a new build has been triggered, Sauron's pipeline will
run applying all plugins pre-configured. The output will be stored in elasticsearch and can be queried afterwards using
the [Kibana Installation](http://localhost:5601).

For more details, check in the next section how the plugin system works.

A build request example can be found below:

```shell script
curl --verbose --location --request POST 'http://localhost:8080/api/v1/build' \
    --header 'Content-Type: application/json' \
    --data-raw '{
      "serviceName": "MyService",
      "repositoryUrl": "https://github.com/gazgeek/springboot-helloworld.git",
      "commitId": "41c7823dddbef43680a0726ccea0631519b9d3c1",
      "buildId": "2b8caa6c-8b55-4b57-b654-5a00d519f409",
      "owner": "Sauron",
      "eventTime": 1586962717770,
      "rollback": false,
      "returnCode": 0,
      "environment": "production",
      "release": "0.0.1",
      "user": "sauron-user",
      "dockerImage": "helloworld:0.0.1",
      "platform": "K8S"
    }'
```

### Visualizing Sauron Data

As mentioned before, Sauron stack uses Elasticsearch + Kibana to respectively store and visualize data. Once the
 Sauron build is done, the information will be available and can be queried using [Kibana](http://localhost:5601).

In order to do that, a index pattern must be created in Kibana using this
[HOWTO](https://www.elastic.co/guide/en/kibana/6.8/tutorial-define-index.html).

Once it is done, you are able to use your data. Sauron provides a default Kibana Dashboard available
[here](https://github.com/freenowtech/sauron/blob/main/sauron-service/kibana/kibana.ndjson).
[Import](https://www.elastic.co/guide/en/kibana/current/dashboard-import-api.html) it and voilà! Enjoy your data!

#### Kibana Dashboard Example

![Sauron Kibana Dashboard](https://github.com/freenowtech/sauron/blob/main/sauron-service/docs/kibana.png)

---

## SAURON PLUGIN SYSTEM

Sauron has an embedded plugin system that allows anyone to insert its own business logic to extract
information during the building/deploy process. It uses the [PF4J](https://github.com/pf4j/pf4j) which is a plugin
framework written in Java, and provides a nice interface to implement an integration in your service.

During the startup Sauron loads all available plugins, and updates them every **5 minutes** using the pre-defined
plugin repository (Local or Artifactory). For more details please refer to
[sauron-service.yml](https://github.com/freenowtech/sauron/blob/main/sauron-service/docker/config/sauron-service.yml).

### Official Plugins

#### [console Output](https://github.com/freenowtech/sauron/tree/main/plugins/console-output)
Prints the DataSet content to `sysout`.

#### [data-sanitizer](https://github.com/freenowtech/sauron/tree/main/plugins/data-sanitizer)
Sanitizes the data before being processed by Sauron pipeline.

#### [git-checkout](https://github.com/freenowtech/sauron/tree/main/plugins/git-checkout)
Checkout the project source code.

#### [dependency-checker](https://github.com/freenowtech/sauron/tree/main/plugins/dependency-checker)
Extracts dependencies information in [CycloneDX](https://cyclonedx.org/#specification-overview) and insert in DataSet.

#### [dependencytrack-publisher](https://github.com/freenowtech/sauron/tree/main/plugins/dependencytrack-publisher)
Publishes the dependencies to our internal [Dependency Track](https://dependencytrack.org) instance.

#### [maven-report](https://github.com/freenowtech/sauron/tree/main/plugins/maven-report)
Retrieves information from `pom.xml` file.

#### [elasticsearch-output](https://github.com/freenowtech/sauron/tree/main/plugins/elasticsearch-output)
Stores the DataSet content into Elasticsearch.

#### [protocw-checker](https://github.com/freenowtech/sauron/tree/main/plugins/protocw-checker)
Checks whether a service is using `protoc`, and the [`protoc wrapper`](https://github.com/freenowtech/protoc-wrapper).

#### [logs-report](https://github.com/freenowtech/sauron/tree/main/plugins/logs-report)
Tells if your service has any logs on elasticsearch. You can configure indexes and time range to search for logs.

#### [kubernetesapi-report](https://github.com/freenowtech/sauron/tree/main/plugins/kubernetesapi-report)
Allows Sauron to query Kubernetes API to retrieve the annotations and labels assigned to the resources, specified in the configuration, and stores the values into the DataSet.

#### [sonarapi-report](https://github.com/freenowtech/sauron/tree/main/plugins/sonarapi-report)
Query [Sonar API](https://docs.sonarqube.org/latest/extend/web-api/) to retrieve your service related data, like Code Coverage, and stores into the DataSet.

#### [thanosapi-report](https://github.com/freenowtech/sauron/tree/main/plugins/thanosapi-report)
Query [Thanos API](https://github.com/thanos-io/thanos/blob/main/docs/components/query.md) to retrieve your service related data(like RPM, Circuit Breaker), and stores into the DataSet.

#### [readme-checker](https://github.com/freenowtech/sauron/tree/main/plugins/readme-checker)
Checks whether a service has or not a README.md file in its root folder.

#### [bcrypt-passwordencoder-checker](https://github.com/freenowtech/sauron/tree/main/plugins/bcrypt-passwordencoder-checker)
Checks whether a service might be using `Bcrypt` to encode the passwords in the API

#### [jaegerapi-report](https://github.com/freenowtech/sauron/tree/main/plugins/jaegerapi-report)
Query [Jaeger API](https://www.jaegertracing.io/docs/1.22/apis/) to extract tracing information


### Creating a New Sauron Plugin

Sauron provides a [maven archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html)
in order to create a standard skeleton of a new plugin. To use that follow the steps below.

#### Install maven archetype

Checkout the [sauron-plugin-archetype](https://github.com/freenowtech/sauron/tree/main/sauron-plugin-archetype)
project:

```bash
$ git clone https://github.com/freenowtech/sauron.git
```

Install the archetype locally and add it to the local archetype catalog:

```bash
$ cd sauron-plugin-archetype
$ mvn clean install
```

#### Create new plugin skeleton

Generate the new plugin using the installed archetype:

```bash
$ mvn org.apache.maven.plugins:maven-archetype-plugin:3.1.2:generate
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------< org.apache.maven:standalone-pom >-------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] --------------------------------[ pom ]---------------------------------
[INFO]
[INFO] >>> maven-archetype-plugin:3.0.1:generate (default-cli) > generate-sources @ standalone-pom >>>
[INFO]
[INFO] <<< maven-archetype-plugin:3.0.1:generate (default-cli) < generate-sources @ standalone-pom <<<
[INFO]
[INFO]
[INFO] --- maven-archetype-plugin:3.0.1:generate (default-cli) @ standalone-pom ---
[INFO] Generating project in Interactive mode
[INFO] No archetype defined. Using maven-archetype-quickstart (org.apache.maven.archetypes:maven-archetype-quickstart:1.0)
Choose archetype:
...
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : sauron-plugin-archetype
Choose archetype:
1:  remote -> com.freenow.sauron:sauron-plugin-archetype (Sauron Plugin Archetype)
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 1
[INFO] Using property: groupId = com.free-now.sauron.plugins
Define value for property 'artifactId': my-plugin
[INFO] Using property: version = 0.0.1-SNAPSHOT
[INFO] Using property: package = com.freenow.sauron.plugins
Define value for property 'className': MyPlugin
Confirm properties configuration:
groupId: com.free-now.sauron.plugins
artifactId: my-plugin
version: 0.0.1-SNAPSHOT
package: com.freenow.sauron.plugins
className: MyPlugin
 Y: :
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: sauron-plugin-archetype:0.0.1-SNAPSHOT
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: com.free-now.sauron.plugins
[INFO] Parameter: artifactId, Value: my-plugin
[INFO] Parameter: version, Value: 0.0.1-SNAPSHOT
[INFO] Parameter: package, Value: com.freenow.sauron.plugins
[INFO] Parameter: packageInPathFormat, Value: com/freenow/sauron/plugins
[INFO] Parameter: package, Value: com.freenow.sauron.plugins
[INFO] Parameter: version, Value: 0.0.1-SNAPSHOT
[INFO] Parameter: groupId, Value: com.free-now.sauron.plugins
[INFO] Parameter: className, Value: MyPlugin
[INFO] Parameter: artifactId, Value: my-plugin
[INFO] Project created from Archetype in dir: /Users/sergio/Documents/sauron/my-plugin
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.041 s
[INFO] Finished at: 2019-04-08T15:10:44+02:00
[INFO] ------------------------------------------------------------------------
```

It will generate a new maven project that follows the structure below:

![](https://github.com/freenowtech/sauron/blob/main/sauron-service/docs/sauron-plugin-structure.png)

Fill in the `MyPlugin.java` class with your desired logic:

```java
package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import org.pf4j.Extension;

@Extension
public class MyPlugin implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        // PLUGIN LOGIC

        return input;
    }
}
```

#### Configuring your new Plugin

In order to provide extra configuration to your plugin, refer to [CONFIGURATION SECTION](#configuration).
After your configuration has been added, it will be available to be used by your plugin in
`PluginsConfigurationProperties` object.
See below an example of how you can use the configuration properties:

The following configuration provides an url to my-plugin plugin generated in step above:

```yaml
sauron:
    plugins:
        my-plugin:
            url: https://my-plugin.com
```

So in order to retrieve this configuration uses the following java code:

```java
@Extension
public class MyPlugin implements SauronExtension
{
    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        properties.getPluginConfigurationProperty("my-plugin", "url").ifPresent(url -> System.out.println(url) );
        return input;
    }
}
```

#### Documenting your Plugin

Sauron Plugin Archetype provides a template of a README that must be filled to document what your plugin does,
inputs, outputs and possible configuration.

#### Deploying your new Plugin

Once the developing and testing process has been done, you can deploy a new version of your plugin, using the
pre-defined plugin repository (Local or Artifactory). For more details please refer to
[sauron-service.yml](https://github.com/freenowtech/sauron/blob/main/sauron-service/docker/config/sauron-service.yml).

The plugin reloading process runs every **5 minutes**. To force a reloading use the `/api/v1/reload` method.

Check [Swagger Documentation](http://localhost:8080/v2/api-docs) for more information.

#### Using your new Plugin

Once your plugin has been deployed and loaded by Sauron Service you can use it in a pipeline which will be then
 applied to new deployments.
Refer to [Sauron Usage](#usage) for detailed information.
