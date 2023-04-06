package io.javaoperatorsdk.jenvtest.sample;

import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer;
import io.javaoperatorsdk.jenvtest.junit.KubeConfig;

import static io.javaoperatorsdk.jenvtest.sample.TestUtils.simpleTest;

class JUnitExtensionOnMethodTest {

  @KubeConfig
  String kubeConfigYaml;

  @Test
  @EnableKubeAPIServer
  void simpleTest1() {
    TestUtils.simpleTest(kubeConfigYaml);
  }

  @Test
  @EnableKubeAPIServer
  void simpleTest2() {
    TestUtils.simpleTest(kubeConfigYaml);
  }
}
