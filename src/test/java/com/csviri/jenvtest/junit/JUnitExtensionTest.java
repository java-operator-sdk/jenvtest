package com.csviri.jenvtest.junit;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import com.csviri.jenvtest.TestUtils;

import static com.csviri.jenvtest.TestUtils.testConfigMap;
import static org.assertj.core.api.Assertions.assertThat;

@EnableAPIServer
class JUnitExtensionTest {

  @Test
  void testCommunication() {
    var client = new KubernetesClientBuilder().build();
    client.resource(testConfigMap()).createOrReplace();
    var cm = client.resource(TestUtils.testConfigMap()).get();

    assertThat(cm).isNotNull();
  }
}
