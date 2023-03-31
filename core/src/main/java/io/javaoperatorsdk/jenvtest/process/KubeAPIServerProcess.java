package io.javaoperatorsdk.jenvtest.process;

import java.io.File;
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
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.*;
import io.javaoperatorsdk.jenvtest.binary.BinaryManager;
import io.javaoperatorsdk.jenvtest.cert.CertManager;

import static io.javaoperatorsdk.jenvtest.KubeAPIServer.STARTUP_TIMEOUT;

public class KubeAPIServerProcess {

  private static final Logger log = LoggerFactory.getLogger(KubeAPIServerProcess.class);
  private static final Logger apiLog = LoggerFactory.getLogger(KubeAPIServerProcess.class
      .getName() + ".APIServerProcessLogs");
  public static final int POLLING_INTERVAL = 150;

  private final CertManager certManager;
  private final BinaryManager binaryManager;
  private final KubeAPIServerConfig config;
  private volatile Process apiServerProcess;
  private volatile boolean stopped = false;
  private final UnexpectedProcessStopHandler processStopHandler;
  private int apiServerPort;

  public KubeAPIServerProcess(CertManager certManager, BinaryManager binaryManager,
      UnexpectedProcessStopHandler processStopHandler,
      KubeAPIServerConfig config) {
    this.certManager = certManager;
    this.binaryManager = binaryManager;
    this.config = config;
    this.processStopHandler = processStopHandler;
  }

  public int startApiServer(int etcdPort) {
    var apiServerBinary = binaryManager.binaries().getApiServer();
    try {
      if (!apiServerBinary.exists()) {
        throw new JenvtestException(
            "Missing binary for API Server on path: " + apiServerBinary.getAbsolutePath());
      }
      apiServerPort = Utils.findFreePort();
      var command = createCommand(apiServerBinary, apiServerPort, etcdPort);
      apiServerProcess = new ProcessBuilder(command)
          .start();
      Utils.redirectProcessOutputToLogger(apiServerProcess.getInputStream(), apiLog);
      Utils.redirectProcessOutputToLogger(apiServerProcess.getErrorStream(), apiLog);
      apiServerProcess.onExit().thenApply(p -> {
        if (!stopped) {
          stopped = true;
          log.error("API Server process stopped unexpectedly");
          this.processStopHandler.processStopped(p);
        }
        return null;
      });
      log.debug("Kube API Server started on port: {} using binaries: {}", apiServerPort,
          apiServerBinary);
      return apiServerPort;
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  private List<String> createCommand(File apiServerBinary, int apiServerPort, int etcdPort) {
    var command = new ArrayList<String>();
    command.add(apiServerBinary.getAbsolutePath());
    command.addAll(config.getApiServerFlags());
    command.addAll(List.of("--cert-dir", config.getJenvtestDir(),
        "--secure-port", "" + apiServerPort,
        "--etcd-servers", "http://0.0.0.0:" + etcdPort,
        "--authorization-mode", "RBAC",
        "--service-account-issuer", "https://localhost",
        "--service-account-signing-key-file", certManager.getAPIServerKeyPath(),
        "--service-account-signing-key-file", certManager.getAPIServerKeyPath(),
        "--service-account-key-file", certManager.getAPIServerKeyPath(),
        "--service-account-issuer", certManager.getAPIServerCertPath(),
        "--disable-admission-plugins", "ServiceAccount",
        "--client-ca-file", certManager.getClientCertPath(),
        "--service-cluster-ip-range", "10.0.0.0/24",
        "--allow-privileged"));
    return command;
  }

  public void waitUntilDefaultNamespaceCreated() {
    try {
      var client = getHttpClient();
      var request = getHttpRequest();
      var startedAt = LocalTime.now();
      while (true) {
        if (ready(client, request)) {
          return;
        }
        if (LocalTime.now().isAfter(startedAt.plus(STARTUP_TIMEOUT, ChronoUnit.MILLIS))) {
          throw new JenvtestException("API Server did not start properly");
        }
        Thread.sleep(POLLING_INTERVAL);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }

  private boolean ready(HttpClient client, HttpRequest request) {
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      log.trace("Ready Response message:{} code: {}", response.body(), response.statusCode());
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

  private HttpRequest getHttpRequest() {
    try {
      return HttpRequest.newBuilder()
          .uri(new URI("https://127.0.0.1:" + apiServerPort + "/readyz"))
          .GET()
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
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
                    SSLEngine engine) throws CertificateException {

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

  public void stopApiServer() {
    if (stopped) {
      return;
    }
    stopped = true;
    if (apiServerProcess != null) {
      apiServerProcess.destroyForcibly();
    }
    log.debug("API Server stopped");
  }

  public int getApiServerPort() {
    return apiServerPort;
  }
}
