package com.csviri.jenvtest;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import com.csviri.jenvtest.junit.EnableKubeAPIServer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@EnableKubeAPIServer
class KubernetesHookHandlingTest {
  public static final String PASSWORD = "secret";

  static Server server = new Server();
  static CertManager certManager =
      new CertManager(KubeAPIServerConfigBuilder.anAPIServerConfig().build().getJenvtestDir());

  @BeforeAll
  static void setup() throws Exception {
    certManager.createCertificatesIfNeeded();

    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.addCustomizer(new SecureRequestCustomizer());
    HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setKeyStore(createKeyStoreFrom(new File(certManager.getClientKeyPath()),
        new File(certManager.getClientCertPath())));
    sslContextFactory.setKeyStorePassword(PASSWORD);
    SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, http11.getProtocol());

    ServerConnector connector = new ServerConnector(server, tls, http11);
    connector.setPort(8443);
    server.addConnector(connector);
    ContextHandler context = new ContextHandler();
    context.setContextPath("/mutate");
    context.setHandler(new AbstractHandler() {
      @Override
      public void handle(String s, Request request, HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse) throws IOException, ServletException {
        System.out.println("!! " + request);
      }
    });
    server.setHandler(context);
    server.start();
  }

  @AfterAll
  static void stopHttpServer() throws Exception {
    server.stop();
  }

  @Test
  void handleMutatingWebhook() {
    var client = new KubernetesClientBuilder().build();
    applyConfig(client);

    var ingress = client.resource(testIngress("test1")).create();
    System.out.println(ingress);
  }

  private void applyConfig(KubernetesClient client) {
    try (var resource =
        KubernetesHookHandlingTest.class.getResourceAsStream("/MutatingWebhookConfig.yaml")) {
      client.load(resource).create();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static KeyStore createKeyStoreFrom(File keyPem, File certPem) {
    try (var certReader = new PEMParser(new InputStreamReader(new FileInputStream(certPem)));
        var keyReader = new PEMParser(new InputStreamReader(new FileInputStream(keyPem)))) {
      var certConverter = new JcaX509CertificateConverter();
      X509Certificate cert =
          certConverter.getCertificate((X509CertificateHolder) certReader.readObject());

      JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
      PrivateKey key =
          keyConverter.getPrivateKey(((PEMKeyPair) keyReader.readObject()).getPrivateKeyInfo());

      KeyStore keystore = KeyStore.getInstance("JKS");
      keystore.load(null);
      keystore.setCertificateEntry("cert-alias", cert);
      keystore.setKeyEntry("key-alias", key, PASSWORD.toCharArray(), new Certificate[] {cert});

      return keystore;
    } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static Ingress testIngress(String name) {
    return new IngressBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withSpec(new IngressSpecBuilder()
            .withIngressClassName("sample")
            .withRules(new IngressRuleBuilder()
                .withHttp(new HTTPIngressRuleValueBuilder()
                    .withPaths(new HTTPIngressPathBuilder()
                        .withPath("/test")
                        .withPathType("Prefix")
                        .withBackend(new IngressBackendBuilder()
                            .withService(new IngressServiceBackendBuilder()
                                .withName("service")
                                .withPort(new ServiceBackendPortBuilder()
                                    .withNumber(80)
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build())
        .build();
  }

}
