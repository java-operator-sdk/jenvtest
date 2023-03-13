package com.csviri.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static com.csviri.jenvtest.TestUtils.testConfigMap;
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
    client.resource(testConfigMap()).create();
    var cm = client.resource(testConfigMap()).get();

    assertThat(cm).isNotNull();

    kubeApi.stop();
  }


}
