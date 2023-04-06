package io.javaoperatorsdk.jenvtest.junit;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;

class JUnitFabric8ClientInjectPerMethod {

  KubernetesClient client;

  @Test
  @EnableKubeAPIServer
  void simpleTest1() {
    TestUtils.simpleTest(client);
  }

  @Test
  @EnableKubeAPIServer
  void simpleTest2() {
    TestUtils.simpleTest(client);
  }
}
