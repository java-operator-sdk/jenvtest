package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

// todo impl in java
public class CertManager {

    private static final Logger log = LoggerFactory.getLogger(CertManager.class);

    public static final String API_SERVER_KEY_NAME = "apiserver.key";
    public static final String API_SERVER_CERT_NAME = "apiserver.crt";

    public static final String CLIENT_KEY_NAME = "client.key";
    public static final String CLIENT_CERT_NAME = "client.crt";

    private String jenvtestDir;

    public CertManager(String jenvtestDir) {
        this.jenvtestDir = jenvtestDir;
    }

    public void ensureAPIServerCertificates() {
        try {
            generateAPIServerCertificates();
            generateUserCertificates();
        } catch (IOException | InterruptedException e) {
            throw new KubeApiException(e);
        }
    }

    // todo generate only if not exists?
    private void generateAPIServerCertificates() throws IOException, InterruptedException {
        var cert = new File(jenvtestDir, API_SERVER_CERT_NAME);
        var key = new File(jenvtestDir, API_SERVER_KEY_NAME);
        if (cert.exists() && key.exists()) {
            return;
        }
        log.debug("Generating API Server certificates");
        Process process = new ProcessBuilder("openssl", "req", "-nodes", "-x509", "-sha256", "-newkey", "rsa:4096", "-keyout", key.getPath(), "-out",
                cert.getPath(),
                "-days", "356", "-subj", "/C=NL/ST=Zuid Holland/L=Rotterdam/O=ACME Corp/OU=IT Dept/CN=example.org",
                "-addext", "subjectAltName = IP:127.0.0.1,DNS:kubernetes," +
                "DNS:kubernetes.default,DNS:kubernetes.default.svc,DNS:kubernetes.default.svc.cluster,DNS:kubernetes.default.svc.cluster.local")
                .start();
        process.waitFor();
    }

    private void generateUserCertificates() throws IOException, InterruptedException {
        var cert = new File(jenvtestDir, CLIENT_CERT_NAME);
        var key = new File(jenvtestDir, CLIENT_KEY_NAME);
        if (cert.exists() && key.exists()) {
            return;
        }
        log.debug("Generating Client certificates");
        var process = new ProcessBuilder("openssl", "req", "-nodes", "-x509", "-sha256", "-newkey",
                "rsa:4096", "-keyout", key.getPath(), "-out", cert.getPath(), "-days", "356", "-subj",
                "/C=NL/ST=Zuid Holland/L=Rotterdam/O=system:masters/OU=IT Dept/CN=jenvtest")
                .start();
        process.waitFor();
    }

    public String getClientCertPath() {
        return new File(jenvtestDir, CLIENT_CERT_NAME).getAbsolutePath();
    }

    public String getClientKeyPath() {
        return new File(jenvtestDir, CLIENT_KEY_NAME).getAbsolutePath();
    }

    public String getAPIServerKeyPath() {
        return new File(jenvtestDir, API_SERVER_KEY_NAME).getAbsolutePath();
    }

    public String getAPIServerCertPath() {
        return new File(jenvtestDir, API_SERVER_CERT_NAME).getAbsolutePath();
    }
}
