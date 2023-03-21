package io.javaoperatorsdk.jenvtest;

import org.junit.jupiter.api.Test;

import static io.javaoperatorsdk.jenvtest.TestUtils.NON_LATEST_API_SERVER_VERSION;
import static io.javaoperatorsdk.jenvtest.TestUtils.simpleTest;

class KubeApiServerTest {

  @Test
  void trivialCase() {
    testWithAPIServer(new KubeAPIServer());
  }

  @Test
  void apiServerWithSpecificVersion() {
    testWithAPIServer(new KubeAPIServer(KubeAPIServerConfigBuilder.anAPIServerConfig()
        .withApiServerVersion(NON_LATEST_API_SERVER_VERSION)
        .build()));
  }


  void testWithAPIServer(KubeAPIServer kubeApi) {
    kubeApi.start();
    simpleTest();
    kubeApi.stop();
  }
}
