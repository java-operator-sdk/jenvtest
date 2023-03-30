package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static io.javaoperatorsdk.jenvtest.TestUtils.NON_LATEST_API_SERVER_VERSION;
import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;
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
        .withApiServerVersion("1.25.*")
        .build());
    kubeApi.start();
    var client = new KubernetesClientBuilder().build();
    TestUtils.simpleTest(client);
    assertThat(client.getKubernetesVersion().getMinor()).isEqualTo("25");

    kubeApi.stop();
  }

  void testWithAPIServer(KubeAPIServer kubeApi) {
    kubeApi.start();
    simpleTest();
    kubeApi.stop();
  }
}
