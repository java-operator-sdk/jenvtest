package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;
import io.javaoperatorsdk.jenvtest.junit.KubeConfig;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

@EnableKubeAPIServer
class JUnitInjectKubeConfig {

  /** needs to be static if shared between tests */
  @KubeConfig
  static String kubeConfigYaml;

  @Test
  void testCommunication1() {
    var client = new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeConfigYaml))
        .build();
    simpleTest(client);
  }

  @Test
  void testCommunication2() {
    var client = new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeConfigYaml))
        .build();
    simpleTest(client);
  }
}
