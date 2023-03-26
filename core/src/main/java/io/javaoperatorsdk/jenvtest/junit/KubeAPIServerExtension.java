package io.javaoperatorsdk.jenvtest.junit;

import java.lang.reflect.AnnotatedElement;
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
    startIfAnnotationPresent(extensionContext);
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    stopIfAnnotationPresent(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    startIfAnnotationPresent(extensionContext);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    stopIfAnnotationPresent(extensionContext);
  }

  private void startIfAnnotationPresent(ExtensionContext extensionContext) {
    extensionContext.getElement().ifPresent(ae -> {
      var annotation = getExtensionAnnotationInstance(ae);
      annotation.ifPresent(this::startApiServer);
    });
  }

  private void startApiServer(EnableKubeAPIServer annotation) {
    kubeApiServer = new KubeAPIServer(annotationToConfig(annotation));
    kubeApiServer.start();
  }


  private void stopIfAnnotationPresent(ExtensionContext extensionContext) {
    extensionContext.getElement().ifPresent(ae -> {
      var annotation = getExtensionAnnotationInstance(ae);
      annotation.ifPresent(a -> kubeApiServer.stop());
    });
  }

  private KubeAPIServerConfig annotationToConfig(EnableKubeAPIServer annotation) {
    var builder = KubeAPIServerConfigBuilder.anAPIServerConfig();
    var version = annotation.kubeAPIVersion();
    if (!NOT_SET.equals(version)) {
      builder.withApiServerVersion(version);
    }
    if (annotation.apiServerFlags().length > 0) {
      builder.withApiServerFlags(List.of(annotation.apiServerFlags()));
    }
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
