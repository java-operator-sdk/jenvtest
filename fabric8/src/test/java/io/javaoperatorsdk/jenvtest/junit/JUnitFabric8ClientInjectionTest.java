package io.javaoperatorsdk.jenvtest.junit;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static io.javaoperatorsdk.jenvtest.junit.TestUtils.simpleTest;

@EnableKubeAPIServer
class JUnitFabric8ClientInjectionTest {

  static KubernetesClient client;

  // old approach also works
  @KubeConfig
  static String configYaml;

  @Test
  void testClientInjection() {
    simpleTest(client);
  }

  @Test
  void testKubeConfigInjectionAlsoWorks() {
    simpleTest(new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(configYaml)).build());
  }
}
