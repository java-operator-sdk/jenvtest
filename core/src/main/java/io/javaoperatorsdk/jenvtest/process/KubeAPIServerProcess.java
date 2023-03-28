package io.javaoperatorsdk.jenvtest.process;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.*;
import io.javaoperatorsdk.jenvtest.binary.BinaryManager;

import static io.javaoperatorsdk.jenvtest.KubeAPIServer.STARTUP_TIMEOUT;

public class KubeAPIServerProcess {

  public static final int REQUEST_WAIT_TIMEOUT = 500;
  private static final Logger log = LoggerFactory.getLogger(KubeAPIServerProcess.class);
  private static final Logger apiLog = LoggerFactory.getLogger(KubeAPIServerProcess.class
      .getName() + ".APIServerProcessLogs");
  public static final int POLLING_INTERVAL = 250;

  private final CertManager certManager;
  private final BinaryManager binaryManager;
  private final KubeAPIServerConfig config;
  private volatile Process apiServerProcess;
  private volatile boolean stopped = false;
  private final UnexpectedProcessStopHandler processStopHandler;

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
      var apiServerPort = Utils.findFreePort();
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
      log.debug("API Server started");
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
      var startedAt = LocalTime.now();
      while (true) {
        if (defaultNamespaceExists()) {
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

  private boolean defaultNamespaceExists() {
    try {
      var proc = new ProcessBuilder(binaryManager.binaries().getKubectl().getPath(), "get", "ns",
          "default").start();
      AtomicBoolean defaultFound = new AtomicBoolean(false);
      var procIsReader = new Thread(() -> {
        log.debug("Starting proc waiter thread.");
        try (Scanner sc = new Scanner(proc.getInputStream())) {
          while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.contains("default")) {
              defaultFound.set(true);
              return;
            }
          }
        }
      });
      procIsReader.start();
      procIsReader.join(REQUEST_WAIT_TIMEOUT);
      return defaultFound.get();
    } catch (IOException e) {
      throw new JenvtestException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
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
}
