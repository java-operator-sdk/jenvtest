package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class APIServer {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);

    private final APIServerConfig config;
    private BinaryManager binaryManager;
    private CertManager certManager;
    private Process etcdProcess;
    private Process apiServerProcess;

    public APIServer() {
        this(new APIServerConfig());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryManager = new BinaryManager(config.getJenvtestDirectory());
        this.certManager = new CertManager(config.getJenvtestDirectory());
    }

    public void start() {
        log.debug("Stating. Using jenvtest dir: {}", config.getJenvtestDirectory());
        prepareLogDirectory();
        cleanPreviousState();
        startEtcd();
        startApiServer();
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

    public void stop() {
        stopApiServer();
        stopEtcd();
    }

    private void startApiServer() {
        certManager.ensureAPIServerCertificates();
        var apiServerBinary = binaryManager.binaries().getApiServer();
        try {
            if (!apiServerBinary.exists()) {
                throw new KubeApiException("Missing binary for API Server on path: " + apiServerBinary.getAbsolutePath());
            }
            apiServerProcess = new ProcessBuilder(apiServerBinary.getAbsolutePath(),
                    "--cert-dir", config.getJenvtestDirectory(), "--etcd-servers",
                    "http://0.0.0.0:2379", "--authorization-mode", "RBAC", "--service-account-issuer",
                    "https://localhost", "--service-account-signing-key-file", certManager.getAPIServerKeyPath(),
                    "--service-account-signing-key-file", certManager.getAPIServerKeyPath(),
                    "--service-account-key-file", certManager.getAPIServerKeyPath(), "--service-account-issuer",
                    certManager.getAPIServerCertPath(),
                    "--disable-admission-plugins", "ServiceAccount", "--client-ca-file", certManager.getClientCertPath())
                    .redirectOutput(new File(config.logDirectory(), "apiserver.logs"))
                    .redirectError(new File(config.logDirectory(), "apiserver.logs"))
                    .start();
            log.debug("API Server started");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopApiServer() {
        if (apiServerProcess != null) {
            apiServerProcess.destroy();
        }
        log.debug("API Server stopped");
    }

    private void cleanPreviousState() {
        // todo
    }


    private void stopEtcd() {
        if (etcdProcess != null) {
            etcdProcess.destroy();
        }
        log.debug("etcd stopped");
    }

    private void startEtcd() {
        var etcdBinary = binaryManager.binaries().getEtcd();
        try {
            if (!etcdBinary.exists()) {
                throw new KubeApiException("Missing binary for etcd on path: " + etcdBinary.getAbsolutePath());
            }
            var logsFile = new File(config.logDirectory(), "etcd.logs");

            // todo config ports
            etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
                    "--listen-client-urls=http://0.0.0.0:2379",
                    "--advertise-client-urls=http://0.0.0.0:2379")
                    .redirectOutput(logsFile)
                    .redirectError(logsFile)
                    .start();
//            etcdProcess.waitFor(5, TimeUnit.SECONDS);
            log.debug("etcd started");
        } catch (IOException e) {
            throw new KubeApiException(e);
        }
    }
}
