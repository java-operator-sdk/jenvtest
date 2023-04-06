# User Documentation

jenvtest is a relatively simple test that support integration testing with Kubernetes API Server.
The whole concept and implementation is relatively simple, this doc describes some high level concepts
and provides some more detailed information.

## Samples

Samples are provided in form of unit tests in the [sample package](https://github.com/java-operator-sdk/jenvtest/tree/main/core/src/test/java/io/javaoperatorsdk/jenvtest/sample)
in core.

For fabric8 client in [this package](https://github.com/java-operator-sdk/jenvtest/tree/main/fabric8/src/test/java/io/javaoperatorsdk/jenvtest/junit/sample)

## Configuration Options

See available configuration options documented in [KubeAPIServerConfig](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/KubeAPIServerConfig.java#L6-L6)

Not all those properties can be overridden using [`@EnableKubeAPIServer`](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/junit/EnableKubeAPIServer.java)
annotation, since might not make sense to do it for an individual test case. However, those can be passed to
[`KubeApiServer`](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/junit/EnableKubeAPIServer.java)
and also configured globally using environment variables, see [KubeAPIServerConfigBuilder](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/KubeAPIServerConfigBuilder.java)

### Updating kube config file

In general, it is not advised but if instructed kube config file (~/kube/config) is updated by the framework.
See related property in [`@EnableKubeAPIServer`](https://github.com/java-operator-sdk/jenvtest/blob/main/core/src/main/java/io/javaoperatorsdk/jenvtest/junit/EnableKubeAPIServer.java#L27-L27)
annotation. The config file is automatically cleaned up on stop.   

## How does it work

In the background Kubernetes and etcd (and kubectl) binaries are downloaded if not found locally.
All the certificates for the Kube API Server and for the client is generated. 
The client certificates are generated with group `system:masters`; 

## Downloading binaries

Binaries are downloaded automatically under ~/.jenvtest/k8s/[target-platform-and-version] if no binary found locally.
If there are multiple binaries found, the latest if selected (unless a target version is not specified).

Also [`setup-envtest`](https://pkg.go.dev/sigs.k8s.io/controller-runtime/tools/setup-envtest#section-readme) can be used
to download binaries manually. By executing `setup-envtest use --bin-dir ~/.jenvtest` will download the latest required
binaries to the default directory. This is useful if always running the tests in offline mode.
