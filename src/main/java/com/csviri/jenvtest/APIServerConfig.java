package com.csviri.jenvtest;

import java.io.File;
import java.util.Optional;

public class APIServerConfig {

    public static final String JENVTEST_DOWNLOAD_BINARIES = "JENVTEST_DOWNLOAD_BINARIES";
    public static final String JENVTEST_DIR_ENV_VAR = "JENVTEST_DIR";

    public static final String DIRECTORY_NAME = ".jenvtest";


    /**
     * Set directory where binaries and other assets are present. Default is ~/.jenvtest.
     **/
    private String jenvtestDir;

    /**
     * If not set the latest binary will be selected automatically
     * Sample: 1.26.1, 1.25.0.
     */
    private String apiServerVersion;

    /**
     * If true, tries to download binaries. If the apiServerVersion is not set and some local binaries found
     * won't try to download them again.
     * */
    private boolean downloadBinaries = true;

    public APIServerConfig() {
        var jenvtestDirFromEnvVar = System.getenv(JENVTEST_DIR_ENV_VAR);
        if (jenvtestDirFromEnvVar != null) {
            this.jenvtestDir = jenvtestDirFromEnvVar;
        } else {
            this.jenvtestDir = new File(System.getProperty("user.home"), DIRECTORY_NAME).getPath();
        }
        var downloadBinariesEnvVal = System.getenv(JENVTEST_DOWNLOAD_BINARIES);
        if (downloadBinariesEnvVal != null) {
            this.downloadBinaries = Boolean.parseBoolean(downloadBinariesEnvVal);
        }
    }

    public String getJenvtestDirectory() {
        return jenvtestDir;
    }

    public APIServerConfig setJenvtestDir(String jenvtestDir) {
        this.jenvtestDir = jenvtestDir;
        return this;
    }

    public Optional<String> getApiServerVersion() {
        return Optional.ofNullable(apiServerVersion);
    }

    public APIServerConfig setApiServerVersion(String apiServerVersion) {
        this.apiServerVersion = apiServerVersion;
        return this;
    }

    public String logDirectory() {
        return new File(jenvtestDir, "logs").getPath();
    }

    public boolean getDownloadBinaries() {
        return downloadBinaries;
    }

    public APIServerConfig setDownloadBinaries(boolean downloadBinaries) {
        this.downloadBinaries = downloadBinaries;
        return this;
    }
}
