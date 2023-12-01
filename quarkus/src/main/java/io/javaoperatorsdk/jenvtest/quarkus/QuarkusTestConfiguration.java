package io.javaoperatorsdk.jenvtest.quarkus;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.KubeAPIServer;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfig;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfigBuilder;
import io.quarkus.arc.DefaultBean;

public class QuarkusTestConfiguration {

  @Produces
  @Singleton
  public KubeAPIServer kubeAPIServer(KubeAPIServerConfig config) {
    KubeAPIServer kubeAPIServer = new KubeAPIServer(config);
    kubeAPIServer.start();

    return kubeAPIServer;
  }

  @Produces
  @Singleton
  @DefaultBean
  public KubeAPIServerConfig kubeAPIServerConfig() {
    return new KubeAPIServerConfigBuilder().build();
  }

  public void stopKubeAPIServer(@Disposes KubeAPIServer kubeAPIServer) {
    kubeAPIServer.stop();
  }

  @Produces
  @Singleton
  public KubernetesClient kubeApiServerClient(KubeAPIServer kubeAPIServer) {
    return new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeAPIServer.getKubeConfigYaml()))
        .build();
  }

  public void closeClient(@Disposes KubernetesClient kubernetesClient) {
    kubernetesClient.close();
  }

}
