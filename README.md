# jenvtest

jenvtest makes it easy to implement integration tests with Kubernetes API Server in Java.
Inspired by [envtest](https://book.kubebuilder.io/reference/envtest.html) in Kubebuilder (Golang).

It runs the API Server binaries directly (without nodes and other components, but with etcd). 
Linux, Windows, Mac is supported.

See also [this blog](https://csviri.medium.com/introducing-jenvtest-kubernetes-api-server-tests-made-easy-for-java-4d02a9bb26d4)
post regarding the motivation and more.

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

See sample unit
test [here](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/test/java/io/javaoperatorsdk/jenvtest/sample/JUnitExtensionSimpleCaseTest.java)

```java

@EnableKubeAPIServer
class JUnitExtensionSimpleCaseTest {

    // Use @KubeConfig annotation to inject kube config yaml to init any client
    @KubeConfig
    static String kubeConfigYaml;

    @Test
    void simpleTestWithTargetVersion() {
        var client = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubeConfigYaml))
                .build();

        client.resource(TestUtils.testConfigMap()).create();
        var cm = client.resource(TestUtils.testConfigMap()).get();

        Assertions.assertThat(cm).isNotNull();
    }
}
```

### Public API

The underlying API can be used directly.
See [KubeApiServer](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/KubeAPIServer.java)

See
it's [usage in a test](https://github.com/java-operator-sdk/jenvtest/blob/main/samples/src/test/java/io/javaoperatorsdk/jenvtest/KubeApiServerTest.java#L12-L35).

```java
class KubeApiServerTest {

    @Test
    void trivialCase() {
        testWithAPIServer(new KubeAPIServer());
    }

    @Test
    void apiServerWithSpecificVersion() {
        testWithAPIServer(new KubeAPIServer(
                KubeAPIServerConfigBuilder.anAPIServerConfig()
                        .withApiServerVersion("1.26.0")
                        .build()));
    }


    void testWithAPIServer(KubeAPIServer kubeApi) {
        kubeApi.start();

        var client = new KubernetesClientBuilder().build();
        client.resource(TestUtils.testConfigMap()).create();
        var cm = client.resource(TestUtils.testConfigMap()).get();

        Assertions.assertThat(cm).isNotNull();

        kubeApi.stop();
    }
}
```

### Fabric8 Kubernetes Client Support 

There is a dedicated support for [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client).

Using dependency:

```xml
<dependency>
    <groupId>io.javaoperatorsdk</groupId>
    <artifactId>jenvtest-fabric8-client</artifactId>
    <version>[version]</version>
    <scope>test</scope>
</dependency>
```

The client can be directly injected to the test. See sample test [here](https://github.com/java-operator-sdk/jenvtest/blob/d5676ceafd278b323532327584401b93a62440ba/fabric8/src/test/java/io/javaoperatorsdk/jenvtest/junit/JUnitFabric8ClientInjectionTest.java).

```java

@EnableKubeAPIServer
class JUnitFabric8ClientInjectionTest {

    static KubernetesClient client;
   
    // emitted code     
}  
```

### Testing Mutation and Validation Webhooks

An additional benefits os running K8S API Server this way, is that it makes easy to test
[Conversion Hooks](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definition-versioning/#webhook-conversion)
and/or
[Dynamic Admission Controllers](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/)

In general, it is a best practice to use additional standard frameworks to implement Kubernetes webhooks,
like [kubernetes-webooks-framework](https://github.com/java-operator-sdk/kubernetes-webooks-framework)
with Quarkus or Spring. However, we demonstrate how it works
in [this test](https://github.com/java-operator-sdk/jenvtest/blob/main/samples/src/test/java/io/javaoperatorsdk/jenvtest/KubernetesMutationHookHandlingTest.java#L53-L53)

### How does it work

In the background Kubernetes and etcd (and kubectl) binaries are downloaded if not found locally.

All the certificates for the Kube API Server and for the client is generated. The client config file
(`~/kube/config`) file is updated, to any client can be used to talk to the API Server.

#### Downloading binaries

Binaries are downloaded automatically under ~/.jenvtest/k8s/[target-platform-and-version] if no binary found locally.
If there are multiple binaries found, the latest if selected (unless a target version is not specified).

Also [`setup-envtest`](https://pkg.go.dev/sigs.k8s.io/controller-runtime/tools/setup-envtest#section-readme) can be used
to download binaries manually. By executing `setup-envtest use --bin-dir ~/.jenvtest` will download the latest required
binaries to the default directory. This is useful if always running the tests in offline mode.

