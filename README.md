# jenvtest

Jenvtest is similar to envtest, to support unit testing with API Server - just for Java:
https://book.kubebuilder.io/reference/envtest.html

It runs the API Server binaries directly (without nodes and other components). Thus, only etcd and Kubernetes API Server.

Project is in early phases, heading towards mvp release.

## Usage 

### Unit Tests

See sample unit test [here](https://github.com/csviri/jenvtest/blob/e22ecef78b916f43e35832e1154da90361db2802/src/test/java/com/csviri/kubeapi/JUnitExtensionTest.java)

```java
@EnableAPIServer
class JUnitExtensionTest {

    @Test
    void testCommunication() {
        var client = new KubernetesClientBuilder().build();
        client.resource(configMap()).createOrReplace();
        var cm = client.resource(configMap()).get();

        assertThat(cm).isNotNull();
    }

    private ConfigMap configMap() {
        return new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("test1")
                        .withNamespace("default")
                        .build())
                .withData(Map.of("key","data"))
                .build();
    }

}
```

### Download binaries

[`setup-envtest`](https://pkg.go.dev/sigs.k8s.io/controller-runtime/tools/setup-envtest#section-readme) can be used
to download binaries.

by executing `setup-envtest use --bin-dir ~/.jenvtest` will download the latest binaries required to the default 
directory.
