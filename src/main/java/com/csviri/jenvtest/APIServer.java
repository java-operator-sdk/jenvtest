package com.csviri.jenvtest;

import com.csviri.jenvtest.binary.BinaryManager;
import com.csviri.jenvtest.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIServer implements UnexpectedProcessStopHandler {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);

    public static final int STARTUP_TIMEOUT = 10_000;

    private final APIServerConfig config;
    private final BinaryManager binaryManager;
    private final CertManager certManager;
    private final KubeConfigManager kubeConfigManager;
    private final EtcdProcessManager etcdProcessManager;
    private final APIServerProcessManager apiServerProcessManager;

    public APIServer() {
        this(APIServerConfigBuilder.anAPIServerConfig().build());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryManager = new BinaryManager(config);
        this.certManager = new CertManager(config.getJenvtestDir());
        this.kubeConfigManager = new KubeConfigManager(certManager, binaryManager);
        this.etcdProcessManager = new EtcdProcessManager(binaryManager, this);
        this.apiServerProcessManager = new APIServerProcessManager(certManager, binaryManager, this, config);
    }

    public void start() {
        log.debug("Stating API Server. Using jenvtest dir: {}", config.getJenvtestDir());
        binaryManager.initAndDownloadIfRequired();
        certManager.createCertificatesIfNeeded();
        etcdProcessManager.cleanEtcdData();
        etcdProcessManager.startEtcd();
        apiServerProcessManager.startApiServer();
        kubeConfigManager.updateKubeConfig();
        apiServerProcessManager.waitUntilDefaultNamespaceCreated();
        log.debug("API Server ready to use");
    }

    public void stop() {
        log.debug("Stopping");
        apiServerProcessManager.stopApiServer();
        etcdProcessManager.stopEtcd();
        kubeConfigManager.cleanupFromKubeConfig();
        etcdProcessManager.cleanEtcdData();
        log.debug("Stopped");
    }

    @Override
    public void processStopped(Process process) {
        stop();
    }
}
