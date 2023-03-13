package com.csviri.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static com.csviri.jenvtest.TestUtils.testConfigMap;
import static org.assertj.core.api.Assertions.assertThat;

class ApiServerTest {

  @Test
  void trivialCase() {
    testWithAPIServer(new APIServer());
  }

  @Test
  void apiServerWithSpecificVersion() {
    testWithAPIServer(new APIServer(APIServerConfigBuilder.anAPIServerConfig()
        .withApiServerVersion("1.26.0")
        .build()));
  }


  void testWithAPIServer(APIServer kubeApi) {
    kubeApi.start();
    var client = new KubernetesClientBuilder().build();
    client.resource(testConfigMap()).create();
    var cm = client.resource(testConfigMap()).get();

    assertThat(cm).isNotNull();

    kubeApi.stop();
  }


}
