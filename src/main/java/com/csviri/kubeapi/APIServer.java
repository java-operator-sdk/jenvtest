package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class APIServer {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);

    private final APIServerConfig config;
    private BinaryHandler binaryHandler;
    private Process etcdProcess;

    public APIServer() {
        this(new APIServerConfig());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryHandler = new BinaryHandler(config.getBinaryDirectory());
    }

    public void start() {
        startEtcd();
        startApiServer();
    }

    public void stop() {
        stopApiServer();
        stopEtcd();
    }

    private void startApiServer() {
    }

    private void stopApiServer() {

    }

    private void stopEtcd() {
        if (etcdProcess != null) {
            etcdProcess.destroy();
        }
        log.debug("etcd stopped");
    }

    private void startEtcd() {
        var etcdBinary = binaryHandler.binaries().getEtcd();
        try {
            if (!etcdBinary.exists()) {
                throw new KubeApiException("Missing binary from etcd on path: " + etcdBinary.getAbsolutePath());
            }
            // todo config ports
            etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
                    "--listen-client-urls=http://0.0.0.0:2379",
                    "--advertise-client-urls=http://0.0.0.0:2379").start();
            log.debug("etcd started");
        } catch (IOException e) {
            throw new KubeApiException(e);
        }
    }
}
