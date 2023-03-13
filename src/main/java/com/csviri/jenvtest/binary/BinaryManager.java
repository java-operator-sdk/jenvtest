package com.csviri.jenvtest.binary;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.csviri.jenvtest.JenvtestException;
import com.csviri.jenvtest.KubeAPIServerConfig;
import com.csviri.jenvtest.Utils;

import static com.csviri.jenvtest.binary.Binaries.*;

public class BinaryManager {

  public static final String BINARY_LIST_DIR = "k8s";

  private Binaries binaries;
  private final KubeAPIServerConfig config;
  private final BinaryDownloader downloader;
  private final OSInfo osInfoProvider;

  public BinaryManager(KubeAPIServerConfig config) {
    this.config = config;
    this.osInfoProvider = new OSInfo();
    this.downloader = new BinaryDownloader(config.getJenvtestDir(), osInfoProvider);
  }

  public void initAndDownloadIfRequired() {
    Optional<File> maybeBinaryDir = findTargetBinariesIfAvailable();
    File binaryDir = maybeBinaryDir.orElse(null);

    if (maybeBinaryDir.isEmpty()) {
      if (!config.isDownloadBinaries()) {
        throw new JenvtestException("Binaries cannot be found, and download is turned off");
      }
      binaryDir = config.getApiServerVersion().isEmpty() ? downloader.downloadLatest()
          : downloader.download(config.getApiServerVersion().get());
    }
    initBinariesPojo(binaryDir);
  }

  private void initBinariesPojo(File binaryDir) {
    this.binaries = new Binaries(new File(binaryDir, ETCD_BINARY_NAME),
        new File(binaryDir, API_SERVER_BINARY_NAME),
        new File(binaryDir, KUBECTL_BINARY_NAME));
    if (!binaries.getApiServer().exists()) {
      throw new JenvtestException(
          "API Server binary not found at path:" + binaries.getApiServer().getPath());
    }
    if (!binaries.getKubectl().exists()) {
      throw new JenvtestException(
          "Kubectl binary not found at path:" + binaries.getKubectl().getPath());
    }
    if (!binaries.getEtcd().exists()) {
      throw new JenvtestException("Etcd binary not found at path:" + binaries.getEtcd().getPath());
    }
  }

  public Binaries binaries() {
    if (binaries == null) {
      throw new JenvtestException("Binaries not found.");
    }
    return binaries;
  }

  private Optional<File> findTargetBinariesIfAvailable() {
    var platformSuffix = Utils.platformSuffix(osInfoProvider);
    if (config.getApiServerVersion().isPresent()) {
      var targetVersionDir = new File(config.getJenvtestDir(), BINARY_LIST_DIR
          + File.separator + config.getApiServerVersion().get() + platformSuffix);
      if (targetVersionDir.exists()) {
        return Optional.of(targetVersionDir);
      } else {
        return Optional.empty();
      }
    }
    File binariesListDir = new File(config.getJenvtestDir(), BINARY_LIST_DIR);
    if (!binariesListDir.exists()) {
      return Optional.empty();
    }
    var dirVersionList = List.of(binariesListDir.list((dir, name) -> name != null
        && name.endsWith(platformSuffix)))
        .stream().map(s -> s.substring(0, s.indexOf(platformSuffix)))
        .collect(Collectors.toList());

    if (dirVersionList.isEmpty()) {
      return Optional.empty();
    }
    String latest = Utils.getLatestVersion(dirVersionList) + platformSuffix;
    return Optional.of(new File(binariesListDir, latest));
  }

}
