package io.javaoperatorsdk.jenvtest;

import java.util.List;
import java.util.Optional;

public class KubeAPIServerConfig {


  /**
   * Set directory where binaries and other assets are present. Default is ~/.jenvtest.
   **/
  private final String jenvtestDir;

  /**
   * If not set the latest binary will be selected automatically. Sample: '1.26.1', '1.25.0'.
   * Wildcards for patch version is supported, like: '1.25.*'. In this case latest patch version is
   * used.
   */
  private final String apiServerVersion;

  /**
   * If true, does not download binaries. If the apiServerVersion is not set and some local binaries
   * found won't try to download them again.
   */
  private final boolean offlineMode;

  /**
   * <p>
   * Flags to pass to Kube API Server on startup. Key and value are two separated items, like
   * specifying min-request-timeout needs to add in order two values: "--min-request-timeout" and
   * "300" for the actual desired value.
   * </p>
   * <p>
   * See Kubernetes docs for possible flags:
   * <a href="https://kubernetes.io/docs/reference/command-line-tools-reference/kube-apiserver/">API
   * Server Docs</a>
   * </p>
   */
  private final List<String> apiServerFlags;

  /**
   * If kube config (in ~/kube/config ) file should be updated.
   */
  private final boolean updateKubeConfig;

  /**
   * This is mostly not needed, and increases startup time. Might be removed in the future.
   */
  private final boolean waitForEtcdHealthCheckOnStartup;

  /**
   * Startup timeout of Kube API Server (and ETCD if enabled - might adds up in this case), the
   * default value is quite high, to support parallel tests. Tuning is possible based on the actual
   * setup.
   */
  private final int startupTimeout;

  KubeAPIServerConfig(String jenvtestDir, String apiServerVersion, boolean offlineMode,
      List<String> apiServerFlags, boolean updateKubeConfig,
      boolean waitForEtcdHealthCheckOnStartup, int startupTimeout) {
    this.jenvtestDir = jenvtestDir;
    this.apiServerVersion = apiServerVersion;
    this.offlineMode = offlineMode;
    this.apiServerFlags = apiServerFlags;
    this.updateKubeConfig = updateKubeConfig;
    this.waitForEtcdHealthCheckOnStartup = waitForEtcdHealthCheckOnStartup;
    this.startupTimeout = startupTimeout;
  }

  public String getJenvtestDir() {
    return jenvtestDir;
  }

  public Optional<String> getApiServerVersion() {
    return Optional.ofNullable(apiServerVersion);
  }

  public boolean isOfflineMode() {
    return offlineMode;
  }

  public List<String> getApiServerFlags() {
    return apiServerFlags;
  }

  public boolean isUpdateKubeConfig() {
    return updateKubeConfig;
  }

  public boolean isWaitForEtcdHealthCheckOnStartup() {
    return waitForEtcdHealthCheckOnStartup;
  }

  public int getStartupTimeout() {
    return startupTimeout;
  }
}
