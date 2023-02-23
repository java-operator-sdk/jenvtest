package com.csviri.kubeapi;

import java.io.File;

public class APIServerConfig {

    public static final String KUBE_API_DEFAULT_DIRECTORY_NAME = ".jenvtest";

    private String binaryDirectory;

    public APIServerConfig() {
        this.binaryDirectory = System.getProperty("user.home") + File.separator + KUBE_API_DEFAULT_DIRECTORY_NAME;
    }

    public String getBinaryDirectory() {
        return binaryDirectory;
    }

    public APIServerConfig setBinaryDirectory(String binaryDirectory) {
        this.binaryDirectory = binaryDirectory;
        return this;
    }
}
