package io.javaoperatorsdk.jenvtest.sample;

import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;

import static io.javaoperatorsdk.jenvtest.sample.TestUtils.simpleTest;

@EnableKubeAPIServer
class JUnitExtensionKubeConfigUpdateTest {

  @Test
  void usesConfigFromGenericKubeConfig() {
    simpleTest();
  }
}
