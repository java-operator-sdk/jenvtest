package io.javaoperatorsdk.jenvtest;

import io.javaoperatorsdk.jenvtest.process.EtcdProcess;
import io.javaoperatorsdk.jenvtest.process.KubeAPIServerProcess;
import io.javaoperatorsdk.jenvtest.process.UnexpectedProcessStopHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.binary.BinaryManager;

public class KubeAPIServer implements UnexpectedProcessStopHandler {

  private static final Logger log = LoggerFactory.getLogger(KubeAPIServer.class);

  public static final int STARTUP_TIMEOUT = 10_000;

  private final KubeAPIServerConfig config;
  private final BinaryManager binaryManager;
  private final CertManager certManager;
  private final KubeConfig kubeConfig;
  private final EtcdProcess etcdProcess;
  private final KubeAPIServerProcess kubeApiServerProcess;

  public KubeAPIServer() {
    this(KubeAPIServerConfigBuilder.anAPIServerConfig().build());
  }

  public KubeAPIServer(KubeAPIServerConfig config) {
    this.config = config;
    this.binaryManager = new BinaryManager(config);
    this.certManager = new CertManager(config.getJenvtestDir());
    this.kubeConfig = new KubeConfig(certManager, binaryManager);
    this.etcdProcess = new EtcdProcess(binaryManager, this);
    this.kubeApiServerProcess =
        new KubeAPIServerProcess(certManager, binaryManager, this, config);
  }

  public void start() {
    log.debug("Stating API Server. Using jenvtest dir: {}", config.getJenvtestDir());
    binaryManager.initAndDownloadIfRequired();
    certManager.createCertificatesIfNeeded();
    etcdProcess.cleanEtcdData();
    etcdProcess.startEtcd();
    kubeApiServerProcess.startApiServer();
    kubeConfig.updateKubeConfig();
    kubeApiServerProcess.waitUntilDefaultNamespaceCreated();
    log.debug("API Server ready to use");
  }

  public void stop() {
    log.debug("Stopping");
    kubeApiServerProcess.stopApiServer();
    etcdProcess.stopEtcd();
    kubeConfig.cleanupFromKubeConfig();
    etcdProcess.cleanEtcdData();
    log.debug("Stopped");
  }

  @Override
  public void processStopped(Process process) {
    stop();
  }
}
