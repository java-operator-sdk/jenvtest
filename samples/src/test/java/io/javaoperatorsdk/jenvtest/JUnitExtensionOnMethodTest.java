package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

class JUnitExtensionOnMethodTest {

  @Test
  @EnableKubeAPIServer
  void testCommunication() {
    simpleTest();
  }

  @Test
  @EnableKubeAPIServer(apiServerFlags = {"--min-request-timeout", "1"})
  void testAdditionalFlags() {
    // todo test with informer
    simpleTest();
  }
}
