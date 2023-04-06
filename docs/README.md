

## Configuration Options





## How does it work

In the background Kubernetes and etcd (and kubectl) binaries are downloaded if not found locally.

All the certificates for the Kube API Server and for the client is generated. The client config file
(`~/kube/config`) file is updated, to any client can be used to talk to the API Server.

## Downloading binaries

Binaries are downloaded automatically under ~/.jenvtest/k8s/[target-platform-and-version] if no binary found locally.
If there are multiple binaries found, the latest if selected (unless a target version is not specified).

Also [`setup-envtest`](https://pkg.go.dev/sigs.k8s.io/controller-runtime/tools/setup-envtest#section-readme) can be used
to download binaries manually. By executing `setup-envtest use --bin-dir ~/.jenvtest` will download the latest required
binaries to the default directory. This is useful if always running the tests in offline mode.