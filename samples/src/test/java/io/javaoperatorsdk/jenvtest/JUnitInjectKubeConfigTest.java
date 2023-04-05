package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;
import io.javaoperatorsdk.jenvtest.junit.KubeConfig;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

@EnableKubeAPIServer
class JUnitInjectKubeConfigTest {

  /**
   * Needs to be static if shared between tests. If present in the test, the "~/kube/config" is not
   * updated
   */
  @KubeConfig
  static String kubeConfigYaml;

  @Test
  void testCommunication1() {
    testWithClientFromGeneratedYaml();
  }

  @Test
  void testCommunication2() {
    testWithClientFromGeneratedYaml();
  }

  private static void testWithClientFromGeneratedYaml() {
    var client = new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeConfigYaml))
        .build();
    simpleTest(client);
  }
}
