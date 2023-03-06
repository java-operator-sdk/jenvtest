package com.csviri.kubeapi;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class EtcdProcessManager {

    private static final Logger log = LoggerFactory.getLogger(EtcdProcessManager.class);

    private final BinaryManager binaryManager;
    private final APIServerConfig config;
    private volatile Process etcdProcess;
    private volatile boolean stopped = false;

    public EtcdProcessManager(BinaryManager binaryManager, APIServerConfig config) {
        this.binaryManager = binaryManager;
        this.config = config;
    }

    public void startEtcd() {
        var etcdBinary = binaryManager.binaries().getEtcd();
        try {
            if (!etcdBinary.exists()) {
                throw new KubeApiException("Missing binary for etcd on path: " + etcdBinary.getAbsolutePath());
            }
            var logsFile = new File(config.logDirectory(), "etcd.logs");

            etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
                    "--listen-client-urls=http://0.0.0.0:2379",
                    "--advertise-client-urls=http://0.0.0.0:2379")
                    // todo log to a different logger on debug level
                    .redirectOutput(logsFile)
                    .redirectError(logsFile)
                    .start();
            etcdProcess.onExit().thenApply(p-> {
                if (!stopped) {
                    throw new KubeApiException("Etcd stopped unexpectedly");
                }
                return null;
            });
            log.debug("etcd started");
        } catch (IOException e) {
            throw new KubeApiException(e);
        }
    }

    public void cleanEtcdData() {
        try {
            FileUtils.deleteDirectory(new File("default.etcd"));
        } catch (IOException e) {
            throw new KubeApiException(e);
        }
    }

    public void stopEtcd() {
        stopped = true;
        if (etcdProcess != null) {
            etcdProcess.destroy();
        }
        log.debug("etcd stopped");
    }
}
