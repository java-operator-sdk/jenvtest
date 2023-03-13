package com.csviri.jenvtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csviri.jenvtest.binary.BinaryManager;

public class KubeConfig {

  private static final Logger log = LoggerFactory.getLogger(KubeConfig.class);
  public static final String JENVTEST = "jenvtest";

  private CertManager certManager;
  private BinaryManager binaryManager;

  public KubeConfig(CertManager certManager, BinaryManager binaryManager) {
    this.certManager = certManager;
    this.binaryManager = binaryManager;
  }

  public void updateKubeConfig() {
    log.debug("Updating kubeconfig");
    execWithKubectlConfigAndWait("set-cluster", JENVTEST, "--server=https://127.0.0.1:6443",
        "--certificate-authority=" + certManager.getAPIServerCertPath());
    execWithKubectlConfigAndWait("set-credentials", JENVTEST,
        "--client-certificate=" + certManager.getClientCertPath(),
        "--client-key=" + certManager.getClientKeyPath());
    execWithKubectlConfigAndWait("set-context", JENVTEST, "--cluster=jenvtest",
        "--namespace=default", "--user=jenvtest");
    execWithKubectlConfigAndWait("use-context", JENVTEST);
  }

  public void cleanupFromKubeConfig() {
    log.debug("Cleanig up kubeconfig");
    unset("contexts." + JENVTEST);
    unset("clusters." + JENVTEST);
    unset("users." + JENVTEST);
  }

  private void unset(String target) {
    execWithKubectlConfigAndWait("unset", target);
  }

  private void execWithKubectlConfigAndWait(String... arguments) {
    try {
      List<String> args = new ArrayList<>(arguments.length + 2);
      args.add(binaryManager.binaries().getKubectl().getPath());
      args.add("config");
      args.addAll(List.of(arguments));
      var process = new ProcessBuilder(args).start();
      process.waitFor();
    } catch (IOException e) {
      throw new JenvtestException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }
}
