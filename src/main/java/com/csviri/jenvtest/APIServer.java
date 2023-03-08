package com.csviri.jenvtest;

import com.csviri.jenvtest.binary.BinaryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class APIServer {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);

    public static final int STARTUP_TIMEOUT = 10_000;

    private final APIServerConfig config;
    private final BinaryManager binaryManager;
    private final CertManager certManager;
    private final KubeConfigManager kubeConfigManager;
    private final EtcdProcessManager etcdProcessManager;
    private final APIServerProcessManager apiServerProcessManager;

    public APIServer() {
        this(new APIServerConfig());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryManager = new BinaryManager(config);
        this.certManager = new CertManager(config.getJenvtestDirectory());
        this.kubeConfigManager = new KubeConfigManager(certManager,binaryManager);
        this.etcdProcessManager = new EtcdProcessManager(binaryManager,config);
        this.apiServerProcessManager = new APIServerProcessManager(certManager,binaryManager,config);
    }

    public void start() {
        log.debug("Stating API Server. Using jenvtest dir: {}", config.getJenvtestDirectory());
        binaryManager.initAndDownloadIfRequired();
        prepareLogDirectory();
        etcdProcessManager.cleanEtcdData();
        etcdProcessManager.startEtcd();
        apiServerProcessManager.startApiServer();
        kubeConfigManager.updateKubeConfig();
        apiServerProcessManager.waitUntilDefaultNamespaceCreated();
        log.info("API Server ready to use");
    }

    public void stop() {
        apiServerProcessManager.stopApiServer();
        etcdProcessManager.stopEtcd();
        kubeConfigManager.cleanupFromKubeConfig();
        etcdProcessManager.cleanEtcdData();
        log.debug("Fully stopped.");
    }

    private void prepareLogDirectory() {
        var logDir = new File(config.logDirectory());
        if (!logDir.exists()) {
            var res = logDir.mkdirs();
            if (!res) {
                log.warn("Problem with creating log dir: {}", logDir.getPath());
            }
        }
    }
}
