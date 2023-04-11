package io.javaoperatorsdk.jenvtest.process;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.function.BooleanSupplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;
import io.javaoperatorsdk.jenvtest.binary.BinaryManager;
import io.javaoperatorsdk.jenvtest.cert.CertManager;

import static io.javaoperatorsdk.jenvtest.process.KubeAPIServerProcess.KUBE_API_SERVER;

public class ProcessReadinessChecker {

  private static final Logger log = LoggerFactory.getLogger(ProcessReadinessChecker.class);

  public static final int STARTUP_TIMEOUT = 60_000;
  public static final int POLLING_INTERVAL = 200;


  public void waitUntilDefaultNamespaceAvailable(int apiServerPort,
      BinaryManager binaryManager,
      CertManager certManager) {
    pollWithTimeout(() -> defaultNamespaceExists(apiServerPort, binaryManager, certManager),
        KUBE_API_SERVER);
  }

  private boolean defaultNamespaceExists(int apiServerPort, BinaryManager binaryManager,
      CertManager certManager) {
    try {
      Process process = new ProcessBuilder(binaryManager.binaries().getKubectl().getPath(),
          "--client-certificate=" + certManager.getClientCertPath(),
          "--client-key=" + certManager.getClientKeyPath(),
          "--certificate-authority=" + certManager.getAPIServerCertPath(),
          "--server=https://127.0.0.1:" + apiServerPort,
          "--request-timeout=5s",
          "get", "ns", "default").start();
      return process.waitFor() == 0;
    } catch (IOException e) {
      throw new JenvtestException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }

  public void waitUntilReady(int port, String readyCheckPath, String processName,
      boolean useTLS) {
    var client = getHttpClient();
    var request = getHttpRequest(useTLS, readyCheckPath, port);
    pollWithTimeout(() -> ready(client, request, processName, port), processName);
  }

  private static void pollWithTimeout(BooleanSupplier predicate, String processName) {
    try {
      var startedAt = LocalTime.now();
      while (true) {
        if (predicate.getAsBoolean()) {
          return;
        }
        if (LocalTime.now().isAfter(startedAt.plus(STARTUP_TIMEOUT, ChronoUnit.MILLIS))) {
          throw new JenvtestException(processName + " did not start properly");
        }
        Thread.sleep(POLLING_INTERVAL);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }

  private boolean ready(HttpClient client, HttpRequest request, String processName, int port) {
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      log.debug("Ready Response message:{} code: {} for {} on Port: {}", response.body(),
          response.statusCode(), processName,
          port);
      return response.statusCode() == 200;
    } catch (ConnectException e) {
      // still want to retry
      log.warn("Cannot connect to the server", e);
      return false;
    } catch (IOException e) {
      throw new JenvtestException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }

  private HttpRequest getHttpRequest(boolean useTLS, String readyCheckPath, int port) {
    try {
      return HttpRequest.newBuilder()
          .uri(new URI((useTLS ? "https" : "http") + "://127.0.0.1:" + port + "/" + readyCheckPath))
          .GET()
          .build();
    } catch (URISyntaxException e) {
      throw new JenvtestException(e);
    }
  }

  private static HttpClient getHttpClient() {
    try {
      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(
          null,
          new TrustManager[] {
              new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType,
                    Socket socket) throws CertificateException {

                }

                public X509Certificate[] getAcceptedIssuers() {
                  return null;
                }

                public void checkClientTrusted(
                    final X509Certificate[] a_certificates,
                    final String a_auth_type) {}

                public void checkServerTrusted(
                    final X509Certificate[] a_certificates,
                    final String a_auth_type) {}


                public void checkServerTrusted(
                    final X509Certificate[] a_certificates,
                    final String a_auth_type,
                    final Socket a_socket) {}

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType,
                    SSLEngine engine) {

                }

                public void checkServerTrusted(
                    final X509Certificate[] a_certificates,
                    final String a_auth_type,
                    final SSLEngine a_engine) {}
              }
          },
          null);
      return HttpClient.newBuilder()
          .sslContext(sslContext)
          .build();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new JenvtestException(e);
    }
  }
}
