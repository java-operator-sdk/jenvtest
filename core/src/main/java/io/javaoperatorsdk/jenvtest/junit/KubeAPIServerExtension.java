package io.javaoperatorsdk.jenvtest.junit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;
import io.javaoperatorsdk.jenvtest.KubeAPIServer;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfig;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfigBuilder;

import static io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer.NOT_SET;

public class KubeAPIServerExtension
    implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final Logger log = LoggerFactory.getLogger(KubeAPIServerExtension.class);

  private KubeAPIServer kubeApiServer;

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    var kubeConfigField = getFieldForKubeConfigInjection(extensionContext, true);
    startIfAnnotationPresent(extensionContext, kubeConfigField.isEmpty());
    kubeConfigField.ifPresent(f -> setKubeConfigYamlToField(extensionContext, f));
  }

  private void setKubeConfigYamlToField(ExtensionContext extensionContext, Field kubeConfigField) {
    try {
      kubeConfigField.setAccessible(true);
      kubeConfigField.set(extensionContext.getTestClass().orElseThrow(),
          kubeApiServer.getKubeConfigYaml());
    } catch (IllegalAccessException e) {
      throw new JenvtestException(e);
    }
  }

  private Optional<Field> getFieldForKubeConfigInjection(ExtensionContext extensionContext,
      boolean findStatic) {
    Class<?> clazz = extensionContext.getTestClass().orElseThrow();
    var kubeConfigFields = Arrays.stream(clazz.getDeclaredFields())
        .filter(f -> f.getAnnotationsByType(KubeConfig.class).length > 0)
        .collect(Collectors.toList());
    if (kubeConfigFields.isEmpty()) {
      return Optional.empty();
    }
    if (kubeConfigFields.size() > 1) {
      throw new JenvtestException(
          "More fields annotation with @" + KubeConfig.class.getSimpleName() + " annotation");
    }
    var field = kubeConfigFields.get(0);
    if (!field.getType().equals(String.class)) {
      throw new JenvtestException(
          "Field annotated with @" + KubeConfig.class.getSimpleName() + " is not a String");
    }

    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) != findStatic) {
      return Optional.empty();
    } else {
      return Optional.of(field);
    }
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    stopIfAnnotationPresent(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    startIfAnnotationPresent(extensionContext, true);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    stopIfAnnotationPresent(extensionContext);
  }

  private void startIfAnnotationPresent(ExtensionContext extensionContext,
      boolean updateKubeConfig) {
    extensionContext.getElement().ifPresent(ae -> {
      var annotation = getExtensionAnnotationInstance(ae);
      annotation.ifPresent(a -> startApiServer(a, updateKubeConfig));
    });
  }

  private void startApiServer(EnableKubeAPIServer annotation, boolean updateKubeConfig) {
    kubeApiServer = new KubeAPIServer(annotationToConfig(annotation, updateKubeConfig));
    kubeApiServer.start();
  }


  private void stopIfAnnotationPresent(ExtensionContext extensionContext) {
    extensionContext.getElement().ifPresent(ae -> {
      var annotation = getExtensionAnnotationInstance(ae);
      annotation.ifPresent(a -> kubeApiServer.stop());
    });
  }

  private KubeAPIServerConfig annotationToConfig(EnableKubeAPIServer annotation,
      boolean updateKubeConfig) {
    var builder = KubeAPIServerConfigBuilder.anAPIServerConfig();
    var version = annotation.kubeAPIVersion();
    if (!NOT_SET.equals(version)) {
      builder.withApiServerVersion(version);
    }
    if (annotation.apiServerFlags().length > 0) {
      builder.withApiServerFlags(List.of(annotation.apiServerFlags()));
    }
    builder.withUpdateKubeConfig(updateKubeConfig);
    return builder.build();
  }

  private Optional<EnableKubeAPIServer> getExtensionAnnotationInstance(AnnotatedElement ae) {
    var annotations = Arrays.stream(ae.getAnnotations())
        .filter(a -> a.annotationType().isAssignableFrom(EnableKubeAPIServer.class))
        .collect(Collectors.toList());
    if (annotations.size() > 1) {
      throw new JenvtestException(
          "Only one instance of @EnableKubeAPIServer annotation is allowed");
    } else if (annotations.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of((EnableKubeAPIServer) annotations.get(0));
    }
  }

}
