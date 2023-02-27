package com.csviri.kubeapi;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;

public class APIServer {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);
    public static final int STARTUP_TIMEOUT = 10_000;

    private final APIServerConfig config;
    private BinaryManager binaryManager;
    private CertManager certManager;
    private KubeConfigManager kubeConfigManager;
    private Process etcdProcess;
    private volatile Process apiServerProcess;
    private Thread startupWaiter;
    private volatile boolean startedUpProperly;

    public APIServer() {
        this(new APIServerConfig());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryManager = new BinaryManager(config.getJenvtestDirectory());
        this.certManager = new CertManager(config.getJenvtestDirectory());
        this.kubeConfigManager = new KubeConfigManager(certManager);
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
        kubeConfigManager.cleanupFromKubeConfig();
        cleanEtcdData();
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
            startupWaiter.join(STARTUP_TIMEOUT);
            if (!startedUpProperly) {
                throw new KubeApiException("Something went wrong starting up KubeApi server. Check the logs");
            }
        } catch (InterruptedException e) {
            throw new KubeApiException(e);
        }
    }

    // todo detect if process not started up correctly
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
                    // todo log to a different logger on debug level
                    .redirectOutput(logsFile)
                    .redirectError(logsFile)
                    .start();
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
            var logsFile = new File(config.logDirectory(), "apiserver.logs");
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

            addStartupReadyHandler();
            // todo detect premature termination
//            apiServerProcess.onExit()
            log.debug("API Server started");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addStartupReadyHandler() {
        // alternative would be to use health checks? https://kubernetes.io/docs/reference/using-api/health-checks/
        // waits until fully started, otherwise default namespace might be missing
        this.startupWaiter = new Thread(() -> {
            // todo the scanner is not closed
            Scanner sc = new Scanner(apiServerProcess.getErrorStream());
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
//                    if (line.contains("Caches are synced")) {
                    if (line.contains("all system priority classes are created successfully or already exist")) {
                        startedUpProperly = true;
                        return;
                    }
                }
            });
        this.startupWaiter.start();
    }

}
