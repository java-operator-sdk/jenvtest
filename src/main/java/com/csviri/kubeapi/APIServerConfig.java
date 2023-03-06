package com.csviri.kubeapi;

import java.io.File;

public class APIServerConfig {

    public static final String DIRECTORY_NAME = ".jenvtest";

    private String jenvtestDir;
    // todo support wildcard 1.26.*
    /**
     * Sample: 1.26.1, 1.25.0
     */
    private String apiServerVersion;

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

    public String getApiServerVersion() {
        return apiServerVersion;
    }

    public APIServerConfig setApiServerVersion(String apiServerVersion) {
        this.apiServerVersion = apiServerVersion;
        return this;
    }

    public String logDirectory() {
        return new File(jenvtestDir,"logs").getPath();
    }
}
