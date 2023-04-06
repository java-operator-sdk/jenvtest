package io.javaoperatorsdk.jenvtest.junit;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
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
  private List<ClientInjectionHandler> clientInjectionHandlers;

  public KubeAPIServerExtension() {
    var loader = ServiceLoader.load(ClientInjectionHandler.class);
    clientInjectionHandlers =
        loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    initialize(extensionContext, true);
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    stopIfAnnotationPresent(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    initialize(extensionContext, false);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    stopIfAnnotationPresent(extensionContext);
  }


  private void initialize(ExtensionContext extensionContext, boolean staticContext) {
    var targetInjectors = clientInjectionHandlers.stream()
        .filter(h -> h.isTargetFieldAvailable(extensionContext, staticContext))
        .collect(Collectors.toList());

    startIfAnnotationPresent(extensionContext, targetInjectors.isEmpty());

    targetInjectors.forEach(i -> i.inject(extensionContext, staticContext, kubeApiServer));
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
