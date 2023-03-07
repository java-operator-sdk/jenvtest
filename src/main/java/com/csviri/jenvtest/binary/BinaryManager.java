package com.csviri.jenvtest.binary;

import com.csviri.jenvtest.APIServerConfig;
import com.csviri.jenvtest.JenvtestException;
import com.csviri.jenvtest.VersioningUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.csviri.jenvtest.binary.Binaries.*;

public class BinaryManager {

    public static final String BINARY_LIST_DIR = "k8s";
    public static final String PLATFORM_SUFFIX = "-" + VersioningUtils.getOSName() + "-" + VersioningUtils.getOSArch();

    private Binaries binaries;
    private final APIServerConfig config;

    public BinaryManager(APIServerConfig config) {
        this.config = config;
    }

    public void initAndDownloadIfRequired() {
        Optional<File> maybeBinaryDir = targetBinaryDir();
        File binaryDir = maybeBinaryDir.orElse(null);

        if (maybeBinaryDir.isEmpty()) {
            if (!config.getDownloadBinaries()) {
                throw new JenvtestException("Binaries cannot be found, and download is turned off");
            }
            BinaryDownloader downloader = new BinaryDownloader(config.getJenvtestDirectory());
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

    private Optional<File> targetBinaryDir() {
        if (config.getApiServerVersion() != null) {
            return Optional.of(new File(config.getJenvtestDirectory(), BINARY_LIST_DIR
                    + File.separator + config.getApiServerVersion() + PLATFORM_SUFFIX));
        }
        File binariesListDir = new File(config.getJenvtestDirectory(), BINARY_LIST_DIR);
        var dirVersionList = List.of(binariesListDir.list((dir, name) -> name.endsWith(PLATFORM_SUFFIX)))
                .stream().map(s -> s.substring(0, s.indexOf(PLATFORM_SUFFIX)))
                .collect(Collectors.toList());

        if (dirVersionList.isEmpty()) {
            return Optional.empty();
        }
        String latest = VersioningUtils.getLatestVersion(dirVersionList) + PLATFORM_SUFFIX;
        return Optional.of(new File(binariesListDir, latest));
    }

}
