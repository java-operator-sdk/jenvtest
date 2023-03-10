package com.csviri.jenvtest.binary;

import com.csviri.jenvtest.APIServerConfig;
import com.csviri.jenvtest.JenvtestException;
import com.csviri.jenvtest.Utils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.csviri.jenvtest.binary.Binaries.*;

public class BinaryManager {

    public static final String BINARY_LIST_DIR = "k8s";
    public static final String PLATFORM_SUFFIX = "-" + Utils.getOSName() + "-" + Utils.getOSArch();

    private Binaries binaries;
    private final APIServerConfig config;
    private BinaryDownloader downloader;

    public BinaryManager(APIServerConfig config) {
        this.config = config;
        downloader = new BinaryDownloader(config.getJenvtestDir());
    }

    public void initAndDownloadIfRequired() {
        Optional<File> maybeBinaryDir = findLatestBinariesAvailable();
        File binaryDir = maybeBinaryDir.orElse(null);

        if (maybeBinaryDir.isEmpty()) {
            if (!config.isDownloadBinaries()) {
                throw new JenvtestException("Binaries cannot be found, and download is turned off");
            }
            binaryDir = config.getApiServerVersion().isEmpty() ?
                   downloader.downloadLatest()
                   : downloader.download(config.getApiServerVersion().get());
        }
        initBinariesPojo(binaryDir);
    }

    private void initBinariesPojo(File binaryDir) {
        this.binaries = new Binaries(new File(binaryDir, ETCD_BINARY_NAME),
                new File(binaryDir, API_SERVER_BINARY_NAME),
                new File(binaryDir, KUBECTL_BINARY_NAME));
        if (!binaries.getApiServer().exists()) {
            throw new JenvtestException("API Server binary not found at path:" + binaries.getApiServer().getPath());
        }
        if (!binaries.getKubectl().exists()) {
            throw new JenvtestException("Kubectl binary not found at path:" + binaries.getKubectl().getPath());
        }
        if (!binaries.getEtcd().exists()) {
            throw new JenvtestException("Etcd binary not found at path:" + binaries.getEtcd().getPath());
        }
    }

    public Binaries binaries() {
        if (binaries == null) {
            throw new JenvtestException("Binaries not found.");
        }
        return binaries;
    }

    private Optional<File> findLatestBinariesAvailable() {
        if (config.getApiServerVersion().isPresent()) {
            return Optional.of(new File(config.getJenvtestDir(), BINARY_LIST_DIR
                    + File.separator + config.getApiServerVersion() + PLATFORM_SUFFIX));
        }
        File binariesListDir = new File(config.getJenvtestDir(), BINARY_LIST_DIR);
        if (!binariesListDir.exists()) {
            return Optional.empty();
        }
        var dirVersionList = List.of(binariesListDir.list((dir, name) -> name.endsWith(PLATFORM_SUFFIX)))
                .stream().map(s -> s.substring(0, s.indexOf(PLATFORM_SUFFIX)))
                .collect(Collectors.toList());

        if (dirVersionList.isEmpty()) {
            return Optional.empty();
        }
        String latest = Utils.getLatestVersion(dirVersionList) + PLATFORM_SUFFIX;
        return Optional.of(new File(binariesListDir, latest));
    }

}
