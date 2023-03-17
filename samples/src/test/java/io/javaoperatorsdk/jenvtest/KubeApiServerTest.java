package io.javaoperatorsdk.jenvtest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class KubeApiServerTest {

  @Test
  void trivialCase() {
    testWithAPIServer(new KubeAPIServer());
  }

  @Test
  void apiServerWithSpecificVersion() {
    testWithAPIServer(new KubeAPIServer(KubeAPIServerConfigBuilder.anAPIServerConfig()
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
