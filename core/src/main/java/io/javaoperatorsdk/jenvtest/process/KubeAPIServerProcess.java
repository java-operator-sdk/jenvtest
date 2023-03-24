package io.javaoperatorsdk.jenvtest.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.*;
import io.javaoperatorsdk.jenvtest.binary.BinaryManager;

public class KubeAPIServerProcess {

  private static final Logger log = LoggerFactory.getLogger(KubeAPIServerProcess.class);
  private static final Logger apiLog = LoggerFactory.getLogger(KubeAPIServerProcess.class
      .getName() + ".APIServerProcessLogs");

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
      AtomicBoolean started = new AtomicBoolean(false);
      var proc = new ProcessBuilder(binaryManager.binaries().getKubectl().getPath(), "get", "ns",
          "--watch").start();
      var procWaiter = new Thread(() -> {
        try (Scanner sc = new Scanner(proc.getInputStream())) {
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
      procWaiter.join(KubeAPIServer.STARTUP_TIMEOUT);
      if (!started.get()) {
        throw new JenvtestException("API Server did not start properly");
      }
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
