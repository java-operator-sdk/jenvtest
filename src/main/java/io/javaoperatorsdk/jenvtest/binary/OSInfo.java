package io.javaoperatorsdk.jenvtest.binary;

public class OSInfo {

  public String getOSName() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return "windows";
    } else if (os.contains("mac")) {
      return "darwin";
    } else {
      return os;
    }
  }

  /**
   * Only amd64 and ppc64 binaries available.
   *
   * @return ppc64le if on that architecture otherwise amd64.
   */
  public String getOSArch() {
    var osArch = System.getProperty("os.arch").toLowerCase();
    if (osArch.contains("ppc64")) {
      return "ppc64le";
    } else {
      return "amd64";
    }
  }

}
