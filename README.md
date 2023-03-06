# jenvtest

Jenvtest is similar to envtest, to support unit testing with API Server - just for Java:
https://book.kubebuilder.io/reference/envtest.html

Project is in early phases, heading towards mvp release.

## Usage 

### Download binaries

[`setup-envtest`](https://pkg.go.dev/sigs.k8s.io/controller-runtime/tools/setup-envtest#section-readme) can be used
to download binaries.

by executing `setup-envtest use --bin-dir ~/.jenvtest` will download the latest binaries required to the default 
directory.
