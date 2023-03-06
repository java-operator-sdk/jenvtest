package com.csviri.kubeapi;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class APIServer {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);

    public static final int STARTUP_TIMEOUT = 10_000;

    private final APIServerConfig config;
    private final BinaryManager binaryManager;
    private final CertManager certManager;
    private final KubeConfigManager kubeConfigManager;
    private Process etcdProcess;
    private volatile Process apiServerProcess;
    private volatile boolean stopped = false;

    public APIServer() {
        this(new APIServerConfig());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryManager = new BinaryManager(config.getJenvtestDirectory(),config.getApiServerVersion());
        this.certManager = new CertManager(config.getJenvtestDirectory());
        this.kubeConfigManager = new KubeConfigManager(certManager,binaryManager);
    }

    public void start() {
        log.debug("Stating API Server. Using jenvtest dir: {}", config.getJenvtestDirectory());
        prepareLogDirectory();
        cleanEtcdData();
        startEtcd();
        startApiServer();
        kubeConfigManager.updateKubeConfig();
        waitUntilDefaultNamespaceCreated();
        log.info("API Server ready to use");
    }

    public void stop() {
        stopped = true;
        stopApiServer();
        stopEtcd();
        kubeConfigManager.cleanupFromKubeConfig();
        cleanEtcdData();
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


    private void stopApiServer() {
        if (apiServerProcess != null) {
            apiServerProcess.destroyForcibly();
        }
        log.debug("API Server stopped");
    }

    private void cleanEtcdData() {
        try {
            FileUtils.deleteDirectory(new File("default.etcd"));
        } catch (IOException e) {
            throw new KubeApiException(e);
        }
    }

    private void stopEtcd() {
        if (etcdProcess != null) {
            etcdProcess.destroy();
        }
        log.debug("etcd stopped");
    }

    private void waitUntilDefaultNamespaceCreated() {
        try {
        AtomicBoolean started = new AtomicBoolean(false);
        var proc = new ProcessBuilder(binaryManager.binaries().getKubectl().getPath(),"get","ns","--watch").start();
        var procWaiter = new Thread(() -> {
            try(Scanner sc = new Scanner(proc.getInputStream())){
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.contains("default")) {
                    started.set(true);
                    return;
                }
            }
            }
        });
        procWaiter.start();
        procWaiter.join(APIServer.STARTUP_TIMEOUT);
        if (!started.get()) {
            throw new KubeApiException("API Server did not start properly. Check the log files.");
        }
        } catch (IOException e) {
            throw new KubeApiException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KubeApiException(e);
        }
    }

    private void startEtcd() {
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

    private void startApiServer() {
        certManager.ensureAPIServerCertificates();
        var apiServerBinary = binaryManager.binaries().getApiServer();
        try {
            if (!apiServerBinary.exists()) {
                throw new KubeApiException("Missing binary for API Server on path: " + apiServerBinary.getAbsolutePath());
            }

            apiServerProcess = new ProcessBuilder(apiServerBinary.getAbsolutePath(),
                    "--cert-dir", config.getJenvtestDirectory(),
                    "--etcd-servers", "http://0.0.0.0:2379",
                    "--authorization-mode", "RBAC",
                    "--service-account-issuer", "https://localhost",
                    "--service-account-signing-key-file", certManager.getAPIServerKeyPath(),
                    "--service-account-signing-key-file", certManager.getAPIServerKeyPath(),
                    "--service-account-key-file", certManager.getAPIServerKeyPath(),
                    "--service-account-issuer", certManager.getAPIServerCertPath(),
                    "--disable-admission-plugins", "ServiceAccount",
                    "--client-ca-file", certManager.getClientCertPath(),
                    "--service-cluster-ip-range", "10.0.0.0/24",
                    "--allow-privileged"
            )
                    .start();
            apiServerProcess.onExit().thenApply(p-> {
                if (!stopped) {
                    throw new KubeApiException("APIServer stopped unexpectedly.");
                }
                return null;
            });
            log.debug("API Server started");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
