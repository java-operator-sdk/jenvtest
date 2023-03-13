package com.csviri.jenvtest.binary;

public class OSInfo {

  public String getOSName() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return "windows";
    } else {
      return os;
    }
  }

  public String getOSArch() {
    return System.getProperty("os.arch").toLowerCase();
  }


}
