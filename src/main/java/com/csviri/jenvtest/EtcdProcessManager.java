package com.csviri.jenvtest;

import com.csviri.jenvtest.binary.BinaryManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class EtcdProcessManager {

    private static final Logger log = LoggerFactory.getLogger(EtcdProcessManager.class);
    private static final Logger etcdLog = LoggerFactory.getLogger(EtcdProcessManager.class.getName() + ".etcdProcess");

    private final BinaryManager binaryManager;

    private volatile Process etcdProcess;
    private volatile boolean stopped = false;
    private final UnexpectedProcessStopHandler processStopHandler;

    public EtcdProcessManager(BinaryManager binaryManager, UnexpectedProcessStopHandler processStopHandler) {
        this.binaryManager = binaryManager;
        this.processStopHandler = processStopHandler;
    }

    public void startEtcd() {
        var etcdBinary = binaryManager.binaries().getEtcd();
        try {
            if (!etcdBinary.exists()) {
                throw new JenvtestException("Missing binary for etcd on path: " + etcdBinary.getAbsolutePath());
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
