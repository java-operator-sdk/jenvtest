package io.javaoperatorsdk.jenvtest.junit;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;
import io.javaoperatorsdk.jenvtest.KubeAPIServer;
import io.javaoperatorsdk.jenvtest.KubeAPIServerConfigBuilder;

import static io.javaoperatorsdk.jenvtest.junit.EnableKubeAPIServer.NOT_SET;

public class KubeAPIServerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger log = LoggerFactory.getLogger(KubeAPIServerExtension.class);

  private KubeAPIServer kubeApiServer;

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    String targetVersion = extensionContext.getElement().map(this::annotatedElementToVersion)
        .orElse(null);

    var builder = KubeAPIServerConfigBuilder.anAPIServerConfig();
    if (targetVersion != null) {
      log.debug("Using api version: {}", targetVersion);
      builder.withApiServerVersion(targetVersion);
    }
    kubeApiServer = new KubeAPIServer(builder.build());
    kubeApiServer.start();
  }

  private String annotatedElementToVersion(AnnotatedElement ae) {
    var annotations = Arrays.stream(ae.getAnnotations())
        .filter(a -> a.annotationType().isAssignableFrom(EnableKubeAPIServer.class))
        .collect(Collectors.toList());
    if (annotations.size() > 1) {
      throw new JenvtestException(
          "Only one instance of @EnableKubeAPIServer annotation is allowed");
    }
    EnableKubeAPIServer target = (EnableKubeAPIServer) annotations.get(0);
    var version = target.kubeAPIVersion();
    return NOT_SET.equals(version) ? null : version;
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    kubeApiServer.stop();
  }
}
