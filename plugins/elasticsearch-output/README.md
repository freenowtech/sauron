## Elasticsearch Output Plugin

### Description

This plugin will store in configured elasticsearch the
[DataSet](https://github.com/freenowtech/sauron/blob/main/core/src/main/java/com/freenow/sauron/model/DataSet.java)
object.

### Configuration

This plugin uses the following configuration:

```yaml
sauron.plugins:
    elasticsearch-output:
        elasticsearch:
            host: <elasticsearch host>
            port: <elasticsearch port>
            scheme: <elasticsearch scheme http|https>
```

### Input

- documentId: An optional additional property used along with ```indexName``` indicating which document should be updated.

- indexName: An optional additional property used along with ```documentId``` indicating the index of the document to be updated.

### Output

This plugin does not produces output.