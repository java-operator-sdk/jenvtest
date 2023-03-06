package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class APIServerProcessManager {

    private static final Logger log = LoggerFactory.getLogger(APIServerProcessManager.class);

    private final CertManager certManager;
    private final BinaryManager binaryManager;
    private final APIServerConfig config;
    private volatile Process apiServerProcess;
    private volatile boolean stopped = false;

    public APIServerProcessManager(CertManager certManager, BinaryManager binaryManager,APIServerConfig config) {
        this.certManager = certManager;
        this.binaryManager = binaryManager;
        this.config = config;
    }


    public void startApiServer() {
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

    public void waitUntilDefaultNamespaceCreated() {
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

    public void stopApiServer() {
        stopped = true;
        if (apiServerProcess != null) {
            apiServerProcess.destroyForcibly();
        }
        log.debug("API Server stopped");
    }
}
