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
    # When checks are needed in different clusters:
    #  - set up a kube config file, see https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/
    #  - set up an association between the environment name and the name of a context in the kube config file
    apiClientConfig:
      default: "default"
      environmentName: "kubeConfigContextName"
    selectors:
      pod:
        - label
        - annotation
      deployment:
        - label
        - annotation
    environmentVariablesCheck:
      - MY_ENV_VAR
    # Reading values from Property files:
    #  - [/path/to/file_a.props] - is the path to a file in a POD 
    #    "THE_OUTPUT_KEY" - is the output key to be appended in the input dataset
    #    "the.prop.key.in.the.file" - is the prop key used to extract the value from the property file.  
    propertiesFilesCheck:
      "[/path/to/file_a.props]":
        "THE_OUTPUT_KEY": "the.prop.key.in.the.file"
      "[/path/to/file_b.env]":
        "ANOTHER_OUTPUT_KEY": "the.prop.key.in.the.file"
```

The possible selectors can be found in
[KubernetesResources](https://github.com/freenowtech/sauron/blob/main/plugins/kubernetesapi-report/src/main/java/com/freenow/sauron/plugins/utils/KubernetesResources.java#L5).

## Input

- serviceName: ServiceName that will be used to filter kubernetes resources by `serviceLabel`

## Output

- All the selector's value that can be found assigned to the specified resources
- All the environment variables and its values that were found in the running pod
- All the values found in the property files for the running pod
