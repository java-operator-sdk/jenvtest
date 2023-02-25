package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KubeConfigManager {

    private static final Logger log = LoggerFactory.getLogger(KubeConfigManager.class);
    private CertManager certManager;

    public KubeConfigManager(CertManager certManager) {
        this.certManager = certManager;
    }

    public void updateKubeConfig() {
        log.debug("Updating kubeconfig");
        execAndWait("kubectl", "config", "set-cluster", "jenvtest", "--server=https://127.0.0.1:6443",
                "--certificate-authority=" + certManager.getAPIServerCertPath());
        execAndWait("kubectl", "config", "set-credentials", "jenvtest",
                "--client-certificate=" + certManager.getClientCertPath(), "--client-key=" + certManager.getClientKeyPath());
        execAndWait("kubectl", "config", "set-context", "jenvtest", "--cluster=jenvtest",
                "--namespace=default", "--user=jenvtest");
        execAndWait("kubectl", "config", "use-context", "jenvtest");
    }

    public void cleanupFromKubeConfig() {
        log.debug("Cleanig up kubeconfig");
        unset("contexts.jenvtest");
        unset("clusters.jenvtest");
        unset("users.jenvtest");
    }

    private void unset(String target) {
        execAndWait("kubectl","config","unset", target);
    }

    private void execAndWait(String... arguments) {
        try {
            var process = new ProcessBuilder(arguments).start();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new KubeApiException(e);
        }
    }
}
