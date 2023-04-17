package io.javaoperatorsdk.jenvtest.quarkus;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.KubeAPIServer;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfig;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfigBuilder;

public class QuarkusTestConfiguration {

  public static final String JENVTEST_CLIENT = "jenvtestClient";

  @Produces
  @Singleton
  public KubeAPIServer kubeAPIServer(KubeAPIServerConfig config) {
    KubeAPIServer kubeAPIServer = new KubeAPIServer(config);
    kubeAPIServer.start();

    return kubeAPIServer;
  }

  @Produces
  @Singleton
  public KubeAPIServerConfig kubeAPIServerConfig() {
    return new KubeAPIServerConfigBuilder().build();
  }

  public void stopKubeAPIServer(@Disposes KubeAPIServer kubeAPIServer) {
    kubeAPIServer.stop();
  }

  @Named(JENVTEST_CLIENT)
  @JenvtestClient
  @Produces
  @Singleton
  public KubernetesClient kubeApiServerClient(KubeAPIServer kubeAPIServer) {
    return new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeAPIServer.getKubeConfigYaml()))
        .build();
  }

  public void closeClient(@Named(JENVTEST_CLIENT) @Disposes KubernetesClient kubernetesClient) {
    kubernetesClient.close();
  }

}
