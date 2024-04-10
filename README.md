 :warning:  **_MOVED_**  Jenvtest was renamed to kube-api-test and moved to be part of [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client/blob/main/doc/kube-api-test.md)

---

# jenvtest

jenvtest makes it easy to implement integration tests with Kubernetes API Server in Java.
Inspired by [envtest](https://book.kubebuilder.io/reference/envtest.html) in Kubebuilder (Golang).

It runs the API Server binaries directly (without nodes and other components, but with etcd). 
Linux, Windows, Mac is supported.

See also [this blog](https://csviri.medium.com/introducing-jenvtest-kubernetes-api-server-tests-made-easy-for-java-4d02a9bb26d4)
post regarding the motivation and more.

## Docs 

See more documentation in [docs](https://github.com/java-operator-sdk/jenvtest/tree/main/docs) directory.

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
it's [usage in a test](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/test/java/io/javaoperatorsdk/jenvtest/sample/KubeApiServerTest.java).

```java
class KubeApiServerTest {
    
    @Test
    void apiServerTest() {
        var kubeApi = new KubeAPIServer();
        kubeApi.start();

        var client =  new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubeApi.getKubeConfigYaml()))
                .build();
        
        client.resource(TestUtils.testConfigMap()).create();
        
        var cm = client.resource(TestUtils.testConfigMap()).get();
        Assertions.assertThat(cm).isNotNull();
        
        kubeApi.stop();
    }
}
```

### Fabric8 Kubernetes Client Support 

There is dedicated support for [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client).

Using dependency:

```xml
<dependency>
    <groupId>io.javaoperatorsdk</groupId>
    <artifactId>jenvtest-fabric8-client-support</artifactId>
    <version>[version]</version>
    <scope>test</scope>
</dependency>
```

The client can be directly injected to the test. See sample test [here](https://github.com/java-operator-sdk/jenvtest/blob/main/fabric8/src/test/java/io/javaoperatorsdk/jenvtest/junit/sample/JUnitFabric8ClientInjectionTest.java#L111-L111).

```java

@EnableKubeAPIServer
class JUnitFabric8ClientInjectionTest {

    static KubernetesClient client;
   
    // emitted code     
}  
```

### Support for Parallel Execution in Junit5

Parallel test execution is explicitly supported for JUnit5, in fact the project tests are running parallel. 
Running a new instance for each test case. This speeds up the tests (in our case >75%) in a way that test cases are also
fully isolated from each other. See the [surefire plugin config](https://github.com/csviri/jenvtest/blob/6bb0510208d33cc64938b7a4518ecd0c21de8b26/pom.xml#L222-L237). 

### Testing Mutation and Validation Webhooks

An additional benefits os running K8S API Server this way, is that it makes easy to test
[Conversion Hooks](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definition-versioning/#webhook-conversion)
and/or
[Dynamic Admission Controllers](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/)

In general, it is a best practice to use additional standard frameworks to implement Kubernetes webhooks,
like [kubernetes-webooks-framework](https://github.com/java-operator-sdk/kubernetes-webooks-framework)
with Quarkus or Spring. However, we demonstrate how it works
in [this test](https://github.com/java-operator-sdk/jenvtest/blob/main/samples/src/test/java/io/javaoperatorsdk/jenvtest/KubernetesMutationHookHandlingTest.java#L53-L53)
