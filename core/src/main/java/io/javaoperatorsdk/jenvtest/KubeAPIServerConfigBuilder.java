package io.javaoperatorsdk.jenvtest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class KubeAPIServerConfigBuilder {

  // environment variables
  public static final String JENVTEST_DOWNLOAD_BINARIES = "JENVTEST_OFFLINE_MODE";
  public static final String JENVTEST_DIR_ENV_VAR = "JENVTEST_DIR";
  public static final String JENVTEST_API_SERVER_VERSION_ENV_VAR = "JENVTEST_API_SERVER_VERSION";
  public static final String JENVTEST_WAIT_FOR_ETCD_HEALTH_CHECK =
      "JENVTEST_WAIT_FOR_ETCD_HEALTH_CHECK";

  public static final String DIRECTORY_NAME = ".jenvtest";

  private String jenvtestDir;
  private String apiServerVersion;
  private Boolean offlineMode;
  private boolean updateKubeConfig = false;
  private final List<String> apiServerFlags = new ArrayList<>(0);
  private Boolean waitForEtcdHealthCheckOnStartup;

  public KubeAPIServerConfigBuilder() {}

  public static KubeAPIServerConfigBuilder anAPIServerConfig() {
    return new KubeAPIServerConfigBuilder();
  }

  public KubeAPIServerConfigBuilder withJenvtestDir(String jenvtestDir) {
    this.jenvtestDir = jenvtestDir;
    return this;
  }

  public KubeAPIServerConfigBuilder withApiServerVersion(String apiServerVersion) {
    this.apiServerVersion = apiServerVersion;
    return this;
  }

  public KubeAPIServerConfigBuilder withOfflineMode(boolean downloadBinaries) {
    this.offlineMode = downloadBinaries;
    return this;
  }

  public KubeAPIServerConfig build() {
    if (jenvtestDir == null) {
      var jenvtestDirFromEnvVar = System.getenv(JENVTEST_DIR_ENV_VAR);
      if (jenvtestDirFromEnvVar != null) {
        this.jenvtestDir = jenvtestDirFromEnvVar;
      } else {
        this.jenvtestDir = new File(System.getProperty("user.home"), DIRECTORY_NAME).getPath();
      }
    }
    if (offlineMode == null) {
      var downloadBinariesEnvVal = System.getenv(JENVTEST_DOWNLOAD_BINARIES);
      if (downloadBinariesEnvVal != null) {
        this.offlineMode = Boolean.parseBoolean(downloadBinariesEnvVal);
      } else {
        this.offlineMode = false;
      }
    }
    if (apiServerVersion == null) {
      var apiServerVersionEnvVar = System.getenv(JENVTEST_API_SERVER_VERSION_ENV_VAR);
      if (apiServerVersionEnvVar != null) {
        this.apiServerVersion = apiServerVersionEnvVar;
      }
    }
    if (waitForEtcdHealthCheckOnStartup == null) {
      var waitForEtcdHealthCheckOnStartup = System.getenv(JENVTEST_API_SERVER_VERSION_ENV_VAR);
      if (waitForEtcdHealthCheckOnStartup != null) {
        this.waitForEtcdHealthCheckOnStartup =
            Boolean.parseBoolean(waitForEtcdHealthCheckOnStartup);
      } else {
        this.waitForEtcdHealthCheckOnStartup = true;
      }
    }
    return new KubeAPIServerConfig(jenvtestDir, apiServerVersion, offlineMode, apiServerFlags,
        updateKubeConfig, waitForEtcdHealthCheckOnStartup);
  }

  public KubeAPIServerConfigBuilder withUpdateKubeConfig(boolean updateKubeConfig) {
    this.updateKubeConfig = updateKubeConfig;
    return this;
  }

  public KubeAPIServerConfigBuilder withApiServerFlags(List<String> flags) {
    apiServerFlags.addAll(flags);
    return this;
  }

  public KubeAPIServerConfigBuilder withApiServerFlag(String key, String value) {
    checkKeyPrefix(key);
    apiServerFlags.add(key);
    apiServerFlags.add(value);
    return this;
  }

  public KubeAPIServerConfigBuilder withApiServerFlag(String key) {
    checkKeyPrefix(key);
    apiServerFlags.add(key);
    return this;
  }

  public KubeAPIServerConfigBuilder withWaitForEtcdHealthCheckOnStartup(
      boolean waitForEtcdHealthCheckOnStartup) {
    this.waitForEtcdHealthCheckOnStartup = waitForEtcdHealthCheckOnStartup;
    return this;
  }

  private void checkKeyPrefix(String key) {
    if (!key.startsWith("--")) {
      throw new JenvtestException(
          "Kube API Server flag needs to start with double dash: '--'; Instead found key: " + key);
    }
  }
}
