package com.csviri.jenvtest;

import java.util.Optional;

public class KubeAPIServerConfig {

  /**
   * Set directory where binaries and other assets are present. Default is ~/.jenvtest.
   **/
  private final String jenvtestDir;

  /**
   * If not set the latest binary will be selected automatically Sample: 1.26.1, 1.25.0.
   */
  private final String apiServerVersion;

  /**
   * If true, tries to download binaries. If the apiServerVersion is not set and some local binaries
   * found won't try to download them again.
   */
  private final boolean downloadBinaries;

  KubeAPIServerConfig(String jenvtestDir, String apiServerVersion, boolean downloadBinaries) {
    this.jenvtestDir = jenvtestDir;
    this.apiServerVersion = apiServerVersion;
    this.downloadBinaries = downloadBinaries;
  }

  public String getJenvtestDir() {
    return jenvtestDir;
  }

  public Optional<String> getApiServerVersion() {
    return Optional.ofNullable(apiServerVersion);
  }

  public boolean isDownloadBinaries() {
    return downloadBinaries;
  }
}
