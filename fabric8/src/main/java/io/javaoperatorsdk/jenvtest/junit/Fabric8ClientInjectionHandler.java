package io.javaoperatorsdk.jenvtest.junit;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.jenvtest.JenvtestException;
import io.javaoperatorsdk.jenvtest.KubeAPIServer;

public class Fabric8ClientInjectionHandler implements ClientInjectionHandler {

  public boolean isTargetFieldAvailable(ExtensionContext extensionContext,
      boolean staticField) {
    return getFieldForKubeConfigInjection(extensionContext, staticField).isPresent();
  }

  public void inject(ExtensionContext extensionContext,
      boolean staticField, KubeAPIServer kubeApiServer) {
    var field = getFieldForKubeConfigInjection(extensionContext, staticField).orElseThrow();
    setKubernetesClientToField(extensionContext, field, kubeApiServer);
  }

  private void setKubernetesClientToField(ExtensionContext extensionContext,
      Field kubeConfigField, KubeAPIServer kubeApiServer) {
    try {
      var target = extensionContext.getTestInstance()
          .orElseGet(() -> extensionContext.getTestClass().orElseThrow());
      kubeConfigField.setAccessible(true);
      kubeConfigField.set(target, new KubernetesClientBuilder()
          .withConfig(Config.fromKubeconfig(kubeApiServer.getKubeConfigYaml()))
          .build());
    } catch (IllegalAccessException e) {
      throw new JenvtestException(e);
    }
  }

  @SuppressWarnings("rawtypes")
  public static Optional<Field> getFieldForKubeConfigInjection(ExtensionContext extensionContext,
      boolean staticField) {
    Class<?> clazz = extensionContext.getTestClass().orElseThrow();
    var kubeConfigFields = Arrays.stream(clazz.getDeclaredFields())
        .filter(f -> KubernetesClient.class.isAssignableFrom(f.getType()))
        .collect(Collectors.toList());
    if (kubeConfigFields.isEmpty()) {
      return Optional.empty();
    }
    if (kubeConfigFields.size() > 1) {
      throw new JenvtestException(
          "More fields type KubernetesClient found");
    }
    var field = kubeConfigFields.get(0);
    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) != staticField) {
      return Optional.empty();
    } else {
      return Optional.of(field);
    }
  }
}
