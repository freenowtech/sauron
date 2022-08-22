# Kubernetes Api Report

## Description

This plugin allows Sauron to query Kubernetes api and retrieve the annotations, labels and env vars assigned
to the resources specified in the configuration and dump the value to the 
[DataSet](https://github.com/freenowtech/sauron/blob/main/sauron-core/src/main/java/com/freenow/sauron/model/DataSet.java).

## Configuration

This plugin uses the following configuration:

```yaml
sauron.plugins:
    kubernetesapi-report:
        serviceLabel: "label/service.name" # The label that will used as a selector to find the resource by serviceName
        selectors:
            pod:
                - label
                - annotation
            deployment:
                - label
                - annotation
        environmentVariablesCheck:
            - MY_ENV_VAR
        # When checks are needed in different clusters:
        #  - deploy https://hub.docker.com/r/bitnami/kubectl/ as service in the desired cluster
        #  - set below the url for the cluster     
        apiClientConfig:
          default: "default"
          clusterName: "cluster-url"
```

The possible selectors can be found in
[KubernetesResources](https://github.com/freenowtech/sauron/blob/main/plugins/kubernetesapi-report/src/main/java/com/freenow/sauron/plugins/utils/KubernetesResources.java#L5).

## Input

- serviceName: ServiceName that will be used to filter kubernetes resources by `serviceLabel`

## Output

- All the selector's value that can be found assigned to the specified resources
- All the environment variables and its values that were found in the running pod