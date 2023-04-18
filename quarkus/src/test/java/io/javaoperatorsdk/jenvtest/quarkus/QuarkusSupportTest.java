package io.javaoperatorsdk.jenvtest.quarkus;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class QuarkusSupportTest {

  @Inject
  KubernetesClient client;

  @Test
  void setsUpJenvtestServerAndClient() {
    assertThat(client.getKubernetesVersion().getGitVersion()).startsWith("v1.26");
  }
}
