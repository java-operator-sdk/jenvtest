package io.csviri.jenvtest.junit;

import org.junit.jupiter.api.Test;

import io.csviri.jenvtest.TestUtils;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static io.csviri.jenvtest.TestUtils.testConfigMap;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKubeAPIServer
class JUnitExtensionTest {

  @Test
  void testCommunication() {
    var client = new KubernetesClientBuilder().build();
    client.resource(testConfigMap()).createOrReplace();
    var cm = client.resource(TestUtils.testConfigMap()).get();

    assertThat(cm).isNotNull();
  }
}
