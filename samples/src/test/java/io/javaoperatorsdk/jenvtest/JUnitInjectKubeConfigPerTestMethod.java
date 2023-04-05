package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;
import io.javaoperatorsdk.jenvtest.junit.KubeConfig;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

class JUnitInjectKubeConfigPerTestMethod {

  @KubeConfig
  String kubeConfigYaml;

  @Test
  @EnableKubeAPIServer
  void testCommunication1() {
    testWithClientFromGeneratedYaml();
  }

  @Test
  @EnableKubeAPIServer
  void testCommunication2() {
    testWithClientFromGeneratedYaml();
  }

  private void testWithClientFromGeneratedYaml() {
    var client = new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeConfigYaml))
        .build();
    simpleTest(client);
  }
}
