package io.javaoperatorsdk.jenvtest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;

@EnableKubeAPIServer
class JUnitExtensionTest {

  @Test
  void testCommunication() {
    var client = new KubernetesClientBuilder().build();
    client.resource(TestUtils.testConfigMap()).createOrReplace();
    var cm = client.resource(TestUtils.testConfigMap()).get();

    Assertions.assertThat(cm).isNotNull();
  }
}
