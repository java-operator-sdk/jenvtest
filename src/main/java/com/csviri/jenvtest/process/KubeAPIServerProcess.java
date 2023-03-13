package com.csviri.jenvtest.process;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csviri.jenvtest.*;
import com.csviri.jenvtest.binary.BinaryManager;

public class KubeAPIServerProcess {

  private static final Logger log = LoggerFactory.getLogger(KubeAPIServerProcess.class);
  private static final Logger apiLog = LoggerFactory.getLogger(KubeAPIServerProcess.class
      .getName() + ".apiServerProcess");

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

  public void startApiServer() {
    var apiServerBinary = binaryManager.binaries().getApiServer();
    try {
      if (!apiServerBinary.exists()) {
        throw new JenvtestException(
            "Missing binary for API Server on path: " + apiServerBinary.getAbsolutePath());
      }

      apiServerProcess = new ProcessBuilder(apiServerBinary.getAbsolutePath(),
          "--cert-dir", config.getJenvtestDir(),
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
          "--allow-privileged")
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
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
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
        throw new JenvtestException("API Server did not start properly. Check the log files.");
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
