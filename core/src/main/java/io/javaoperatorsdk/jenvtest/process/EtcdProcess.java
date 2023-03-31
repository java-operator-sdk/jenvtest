package io.javaoperatorsdk.jenvtest.process;

import java.io.File;
import java.io.IOException;

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

  public EtcdProcess(BinaryManager binaryManager,
      UnexpectedProcessStopHandler processStopHandler) {
    this.binaryManager = binaryManager;
    this.processStopHandler = processStopHandler;
  }

  public int startEtcd() {
    var etcdBinary = binaryManager.binaries().getEtcd();
    var port = Utils.findFreePort();
    var peerPort = Utils.findFreePort();
    try {
      if (!etcdBinary.exists()) {
        throw new JenvtestException(
            "Missing binary for etcd on path: " + etcdBinary.getAbsolutePath());
      }
      etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
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
      return port;
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  public void cleanEtcdData() {
    try {
      FileUtils.deleteDirectory(new File("default.etcd"));
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
      etcdProcess.destroy();
    }
    log.debug("etcd stopped");
  }
}
