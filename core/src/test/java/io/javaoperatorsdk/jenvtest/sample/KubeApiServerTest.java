package io.javaoperatorsdk.jenvtest.sample;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.KubeAPIServer;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfigBuilder;

import static io.javaoperatorsdk.jenvtest.sample.TestUtils.NON_LATEST_API_SERVER_VERSION;
import static io.javaoperatorsdk.jenvtest.sample.TestUtils.simpleTest;
import static org.assertj.core.api.Assertions.assertThat;

class KubeApiServerTest {

  @Test
  void trivialCase() {
    testWithAPIServer(new KubeAPIServer());
  }

  @Test
  void apiServerWithSpecificVersion() {
    testWithAPIServer(new KubeAPIServer(KubeAPIServerConfigBuilder.anAPIServerConfig()
        .withApiServerVersion(NON_LATEST_API_SERVER_VERSION)
        .build()));
  }

  @Test
  void usingWildcardVersion() {
    var kubeApi = new KubeAPIServer(KubeAPIServerConfigBuilder.anAPIServerConfig()
        .withApiServerVersion("1.26.*")
        .build());
    kubeApi.start();

    var client = new KubernetesClientBuilder().build();
    TestUtils.simpleTest(client);
    assertThat(client.getKubernetesVersion().getMinor()).isEqualTo("26");

    kubeApi.stop();
  }

  @Test
  void creatingClientFromConfigString() {
    var kubeApi = new KubeAPIServer(KubeAPIServerConfigBuilder.anAPIServerConfig()
        .withUpdateKubeConfig(false)
        .build());
    kubeApi.start();

    var client =
        new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeApi.getKubeConfigYaml()))
            .build();
    TestUtils.simpleTest(client);

    kubeApi.stop();
  }

  void testWithAPIServer(KubeAPIServer kubeApi) {
    kubeApi.start();
    simpleTest();
    kubeApi.stop();
  }
}
