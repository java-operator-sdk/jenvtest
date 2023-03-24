package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

@EnableKubeAPIServer
class JUnitExtensionOnClassTest {

  @Test
  void testCommunication() {
    simpleTest();
  }
}
