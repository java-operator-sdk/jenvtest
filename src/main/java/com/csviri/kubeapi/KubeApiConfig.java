package com.csviri.kubeapi;

import java.io.File;

public class KubeApiConfig {

    public static final String KUBE_API_DEFAULT_DIRECTORY_NAME = "kubeapi";

    private String binaryDirectory;

    public KubeApiConfig() {
        this.binaryDirectory = System.getProperty("java.io.tmpdir") + File.separator + KUBE_API_DEFAULT_DIRECTORY_NAME;
    }

    public String getBinaryDirectory() {
        return binaryDirectory;
    }

    public KubeApiConfig setBinaryDirectory(String binaryDirectory) {
        this.binaryDirectory = binaryDirectory;
        return this;
    }
}
