package io.javaoperatorsdk.jenvtest.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;
import io.javaoperatorsdk.jenvtest.Utils;
import io.javaoperatorsdk.jenvtest.binary.BinaryManager;

public class EtcdProcess {

  private static final Logger log = LoggerFactory.getLogger(EtcdProcess.class);
  private static final Logger etcdLog =
      LoggerFactory.getLogger(EtcdProcess.class.getName() + ".EtcdProcessLogs");

  private final BinaryManager binaryManager;

  private volatile Process etcdProcess;
  private volatile boolean stopped = false;
  private final UnexpectedProcessStopHandler processStopHandler;
  private final boolean waitForHealthCheck;
  private File tempWalDir;
  private File tempDataDir;

  public EtcdProcess(BinaryManager binaryManager,
      UnexpectedProcessStopHandler processStopHandler, boolean waitForHealthCheck) {
    this.binaryManager = binaryManager;
    this.processStopHandler = processStopHandler;
    this.waitForHealthCheck = waitForHealthCheck;
  }

  public int startEtcd() {
    try {
      var etcdBinary = binaryManager.binaries().getEtcd();
      tempWalDir = Files.createTempDirectory("etcdwal").toFile();
      tempDataDir = Files.createTempDirectory("etcddata").toFile();
      log.trace("Using temp wal dir: {} and temp data dir: {}", tempWalDir.getPath(),
          tempDataDir.getPath());
      var port = Utils.findFreePort();
      var peerPort = Utils.findFreePort();

      if (!etcdBinary.exists()) {
        throw new JenvtestException(
            "Missing binary for etcd on path: " + etcdBinary.getAbsolutePath());
      }
      etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
          "-data-dir", tempDataDir.getPath(),
          "-wal-dir", tempWalDir.getPath(),
          "--listen-client-urls", "http://0.0.0.0:" + port,
          "--advertise-client-urls", "http://0.0.0.0:" + port,
          // the below added because of stability
          "--initial-cluster", "default=http://localhost:" + peerPort,
          "--initial-advertise-peer-urls", "http://localhost:" + peerPort,
          "--listen-peer-urls", "http://localhost:" + peerPort)
          .start();
      Utils.redirectProcessOutputToLogger(etcdProcess.getInputStream(), etcdLog);
      Utils.redirectProcessOutputToLogger(etcdProcess.getErrorStream(), etcdLog);
      etcdProcess.onExit().thenApply(p -> {
        if (!stopped) {
          stopped = true;
          log.error("etcd process stopped unexpectedly");
          processStopHandler.processStopped(p);
        }
        return null;
      });
      log.debug("etcd started on port: {}", port);
      if (waitForHealthCheck) {
        waitUntilEtcdHealthy(port);
      }
      return port;
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  private void waitUntilEtcdHealthy(int port) {
    new ProcessReadinessChecker().waitUntilReady(port, "health", "etcd", false);
  }

  public void cleanEtcdData() {
    try {
      FileUtils.deleteDirectory(tempDataDir);
      FileUtils.deleteDirectory(tempWalDir);
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  public void stopEtcd() {
    if (stopped) {
      return;
    }
    stopped = true;
    if (etcdProcess != null) {
      try {
        etcdProcess.destroyForcibly();
        etcdProcess.waitFor();
      } catch (InterruptedException e) {
        throw new JenvtestException(e);
      }
    }
    log.debug("etcd stopped");
  }
}
