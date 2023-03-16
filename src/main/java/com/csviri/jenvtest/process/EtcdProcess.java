package com.csviri.jenvtest.process;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csviri.jenvtest.JenvtestException;
import com.csviri.jenvtest.Utils;
import com.csviri.jenvtest.binary.BinaryManager;

public class EtcdProcess {

  private static final Logger log = LoggerFactory.getLogger(EtcdProcess.class);
  private static final Logger etcdLog =
      LoggerFactory.getLogger(EtcdProcess.class.getName() + ".Process");

  private final BinaryManager binaryManager;

  private volatile Process etcdProcess;
  private volatile boolean stopped = false;
  private final UnexpectedProcessStopHandler processStopHandler;

  public EtcdProcess(BinaryManager binaryManager,
      UnexpectedProcessStopHandler processStopHandler) {
    this.binaryManager = binaryManager;
    this.processStopHandler = processStopHandler;
  }

  public void startEtcd() {
    var etcdBinary = binaryManager.binaries().getEtcd();
    try {
      if (!etcdBinary.exists()) {
        throw new JenvtestException(
            "Missing binary for etcd on path: " + etcdBinary.getAbsolutePath());
      }
      etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
          "--listen-client-urls=http://0.0.0.0:2379",
          "--advertise-client-urls=http://0.0.0.0:2379")
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
      log.debug("etcd started");
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
