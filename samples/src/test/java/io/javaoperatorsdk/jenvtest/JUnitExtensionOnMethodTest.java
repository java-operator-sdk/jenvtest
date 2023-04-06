package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;
import io.javaoperatorsdk.jenvtest.junit.KubeConfig;

import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

class JUnitExtensionOnMethodTest {

  @KubeConfig
  String kubeConfigYaml;

  @Test
  @EnableKubeAPIServer
  void testCommunication() {
    simpleTest(kubeConfigYaml);
  }

  @Test
  @EnableKubeAPIServer
  void testCommunication2() {
    simpleTest(kubeConfigYaml);
  }
}
