package com.csviri.kubeapi;

import java.io.File;

public class APIServerConfig {

    public static final String CONFIG_ROOT_ENV_VAR = "JENVTEST_DIR";
    public static final String DIRECTORY_NAME = ".jenvtest";

    /**
     * Set directory where binaries and other assets are present. Default is ~/.jenvtest.
     **/
    private String jenvtestDir;

    /**
     * Sample: 1.26.1, 1.25.0
     */
    private String apiServerVersion;

    public APIServerConfig() {
        var jenvtestDirFromEnvVar = System.getenv(CONFIG_ROOT_ENV_VAR);
        if (jenvtestDirFromEnvVar != null) {
            this.jenvtestDir = jenvtestDirFromEnvVar;
        } else {
            this.jenvtestDir = new File(System.getProperty("user.home"), DIRECTORY_NAME).getPath();
        }
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
        return new File(jenvtestDir, "logs").getPath();
    }
}
