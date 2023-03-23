package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKubeAPIServer(kubeAPIVersion = TestUtils.NON_LATEST_API_SERVER_VERSION)
class JUnitExtensionTargetVersionTest {

  @Test
  void simpleTestWithTargetVersion() {
    var client = new KubernetesClientBuilder().build();

    simpleTest(client);

    String kubeVersion = client.getKubernetesVersion().getGitVersion().substring(1);
    assertThat(kubeVersion).isEqualTo(TestUtils.NON_LATEST_API_SERVER_VERSION);
  }

}