package com.csviri.jenvtest;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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

    public void createCertificatesIfNeeded() {
        try {
            generateAPIServerCertificates();
            generateUserCertificates();
        } catch (IOException | InterruptedException e) {
            throw new JenvtestException(e);
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
        generateKeyAndCertificate("CN=example.org", new File(jenvtestDir, API_SERVER_KEY_NAME),
                new File(jenvtestDir, API_SERVER_CERT_NAME),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                dns("kubernetes"), dns("kubernetes.default"),
                dns("kubernetes.default.svc"), dns("kubernetes.default.svc.cluster"),
                dns("kubernetes.default.svc.cluster.local"));
    }

    private GeneralName dns(String dns) {
        return new GeneralName(GeneralName.dNSName, dns);
    }

    private void generateUserCertificates() throws IOException, InterruptedException {
        var cert = new File(jenvtestDir, CLIENT_CERT_NAME);
        var key = new File(jenvtestDir, CLIENT_KEY_NAME);
        if (cert.exists() && key.exists()) {
            return;
        }
        log.debug("Generating Client certificates");
        generateKeyAndCertificate("O=system:masters,CN=jenvtest", new File(jenvtestDir, CLIENT_KEY_NAME),
                new File(jenvtestDir, CLIENT_CERT_NAME));
    }


    private static void generateKeyAndCertificate(String dirName, File keyFile, File certFile, GeneralName... generalNames) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

            KeyPair certKeyPair = keyGen.generateKeyPair();
            X500Name name = new X500Name(dirName);
            // If you issue more than just test certificates, you might want a decent serial number schema ^.^
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
            Instant validFrom = Instant.now();
            Instant validUntil = validFrom.plus(365, ChronoUnit.DAYS);

            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    name,
                    serialNumber,
                    Date.from(validFrom), Date.from(validUntil),
                    name, certKeyPair.getPublic());

            if (generalNames.length > 0) {
                builder.addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(generalNames));
            }

            // Finally, sign the certificate:
            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(certKeyPair.getPrivate());
            X509CertificateHolder certHolder = builder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

            try (FileWriter certWriter = new FileWriter(certFile); JcaPEMWriter certPemWriter = new JcaPEMWriter(certWriter);
                 FileWriter keyWriter = new FileWriter(keyFile); JcaPEMWriter keyPemWriter = new JcaPEMWriter(keyWriter)) {
                certPemWriter.writeObject(cert);
                keyPemWriter.writeObject(certKeyPair.getPrivate());
            }
        } catch (NoSuchAlgorithmException | CertificateException | IOException | OperatorCreationException e) {
            throw new JenvtestException(e);
        }
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
