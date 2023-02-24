package com.csviri.kubeapi;

import java.io.File;

public class APIServerConfig {

    public static final String DIRECTORY_NAME = ".jenvtest";

    private String jenvtestDir;

    public APIServerConfig() {
        this.jenvtestDir = System.getProperty("user.home") + File.separator + DIRECTORY_NAME;
    }

    public String getJenvtestDirectory() {
        return jenvtestDir;
    }

    public APIServerConfig setJenvtestDir(String jenvtestDir) {
        this.jenvtestDir = jenvtestDir;
        return this;
    }

    public String logDirectory() {
        return new File(jenvtestDir,"logs").getPath();
    }
}
