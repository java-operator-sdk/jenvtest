package com.csviri.jenvtest.junit;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.csviri.jenvtest.APIServer;

public class APIServerExtension implements BeforeAllCallback, AfterAllCallback {

  private APIServer apiServer;

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    apiServer = new APIServer();
    apiServer.start();
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    apiServer.stop();
  }
}
