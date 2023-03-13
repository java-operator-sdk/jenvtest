package com.csviri.jenvtest.junit;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.csviri.jenvtest.KubeAPIServer;

public class KubeAPIServerExtension implements BeforeAllCallback, AfterAllCallback {

  private KubeAPIServer kubeApiServer;

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    kubeApiServer = new KubeAPIServer();
    kubeApiServer.start();
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    kubeApiServer.stop();
  }
}
