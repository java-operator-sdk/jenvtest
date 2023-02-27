package com.csviri.kubeapi.junit;

import com.csviri.kubeapi.APIServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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
