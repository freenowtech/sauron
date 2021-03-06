# Kubernetes Api Report

## Description

This plugin allows Sauron to query Kubernetes api and retrieve the annotations and labels assigned
to the resources specified in the configuration and dump the value to the 
[DataSet](https://github.com/freenowtech/sauron/blob/main/core/src/main/java/com/freenow/sauron/model/DataSet.java).

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
```

The possible selectors can be found in
[KubernetesResources](https://github.com/freenowtech/sauron/blob/main/plugins/kubernetesapi-report/src/main/java/com/freenow/sauron/plugins/KubernetesResources.java#L5).

## Input

- serviceName: ServiceName that will be used to filter kubernetes resources by `serviceLabel`

## Output

- All the selector's value that can be found assigned to the specified resources