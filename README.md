# jenvtest

jenvtest makes it easy to implement integration tests with Kubernetes API Server in Java. 
Inspired by [envtest](https://book.kubebuilder.io/reference/envtest.html) in go.

It runs the API Server binaries directly (without nodes and other components). Thus, only etcd and Kubernetes API Server.
Linux, Windows, Mac is supported.

Project is in early phases, heading towards mvp release.

## Usage 

Include dependency:

```xml
<dependency>
   <groupId>io.javaoperatorsdk</groupId>
   <artifactId>jenvtest</artifactId>
   <version>[version]</version>
   <scope>test</scope>
</dependency>
```

### In Unit Tests

See sample unit test [here](https://github.com/java-operator-sdk/jenvtest/blob/main/samples/src/test/java/io/javaoperatorsdk/jenvtest/JUnitExtensionTest.java#L10-L10)

```java
 
@EnableKubeAPIServer // Start/Stop Kube API Server in the background
class JUnitExtensionTest {

    @Test
    void testCommunication() {
        // use a Kubernetes client to communicate with the server
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

### API

The underlying API can be used directly. See [KubeApiServer](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/KubeAPIServer.java#L47-L47)

https://github.com/java-operator-sdk/jenvtest/blob/main/samples/src/test/java/io/javaoperatorsdk/jenvtest/KubeApiServerTest.java#L12-L35

### Testing Mutation and Validation Webhooks

An additional benefits os running K8S API Server this way, is that it makes easy to test 
[Conversion Hooks](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definition-versioning/#webhook-conversion) 
and/or
[Dynamic Admission Controllers](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/)

In general using additional standard frameworks to implement webhookhooks is adviced, like [kubernetes-webooks-framework](https://github.com/java-operator-sdk/kubernetes-webooks-framework)
with Quarkus or Spring. However, we demonstrate how it works in [this test](https://github.com/java-operator-sdk/jenvtest/blob/main/samples/src/test/java/io/javaoperatorsdk/jenvtest/KubernetesMutationHookHandlingTest.java#L53-L53)

### How does it work

In the background Kubernetes and etcd (and kubectl) binaries are downloaded if not found locally.

All the certificates for the Kube API Server and for the client is generated. The client config file
(`~/kube/config`) file is updated, to any client can be used to talk to the API Server. 

#### Downloading binaries

Binaries are downloaded automatically under ~/.jenvtest/k8s/[target-platform-and-version].

Also [`setup-envtest`](https://pkg.go.dev/sigs.k8s.io/controller-runtime/tools/setup-envtest#section-readme) can be used
to download binaries manually. By executing `setup-envtest use --bin-dir ~/.jenvtest` will download the latest required
binaries to the default directory.

