package com.csviri.jenvtest.binary;

import com.csviri.jenvtest.JenvtestException;
import com.csviri.jenvtest.VersioningUtils;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BinaryDownloader {

    private static final Logger log = LoggerFactory.getLogger(BinaryDownloader.class);

    private static final String BUCKET_NAME = "kubebuilder-tools";
    private static final String TAR_PREFIX = "kubebuilder-tools-";

    private String jenvtestDir;

    public BinaryDownloader(String jenvtestDir) {
        this.jenvtestDir = jenvtestDir;
    }

    public File download(String version) {
        try {
            String url = "https://storage.cloud.google.com/kubebuilder-tools/kubebuilder-tools-" + version +
                    "-" + VersioningUtils.getOSName() + "-" + VersioningUtils.getOSArch() + ".tar.gz";
            InputStream in = new URL(url).openStream();
            File tempFile = File.createTempFile("kubebuilder-tools", ".tar.gz");
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File dir = createDirForBinaries(version);
            extractFiles(tempFile, dir);
            var deleted = tempFile.delete();
            if (!deleted) {
                log.warn("Unable to delete temp file: {}", tempFile.getPath());
            }
            return dir;
        } catch (IOException e) {
            throw new JenvtestException(e);
        }
    }

    private void extractFiles(File tempFile, File dir) {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))))) {
            var entry = tarIn.getNextTarEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    entry = tarIn.getNextTarEntry();
                } else {
                    extractEntry(entry, dir, tarIn);
                }
            }
        } catch (IOException e) {
            throw new JenvtestException(e);
        }
    }

    private void extractEntry(TarArchiveEntry entry, File dir, TarArchiveInputStream tarIn) throws IOException {
        String name = entry.getName();
        File targetFile;
        if (name.contains(Binaries.KUBECTL_BINARY_NAME)) {
            targetFile = new File(dir, Binaries.KUBECTL_BINARY_NAME);
        } else if (name.contains(Binaries.API_SERVER_BINARY_NAME)) {
            targetFile = new File(dir, Binaries.API_SERVER_BINARY_NAME);
        } else if (name.contains(Binaries.ETCD_BINARY_NAME)) {
            targetFile = new File(dir, Binaries.ETCD_BINARY_NAME);
        } else {
            throw new JenvtestException("Unexpected entry with name: " + entry.getName());
        }
        Files.copy(tarIn, targetFile.toPath());
    }

    private File createDirForBinaries(String version) {
        var dir = new File(jenvtestDir, BinaryManager.BINARY_LIST_DIR + File.separator
                + version + BinaryManager.PLATFORM_SUFFIX);
        if (!dir.mkdirs()) {
            throw new JenvtestException("Cannot created director: " + dir.getPath());
        }
        return dir;
    }

    public File downloadLatest() {
        String latest= findLatestVersion();
        log.info("Downloading latest version binaries: {}",latest);
        return download(latest);
    }

    private String findLatestVersion() {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        var blobs = storage.get(BUCKET_NAME).list();
        var allRelevantVersions = StreamSupport.stream(blobs.iterateAll().spliterator(), false).filter(b ->
                        b.asBlobInfo().getName().contains(VersioningUtils.getOSName())
                                && b.asBlobInfo().getName().contains(VersioningUtils.getOSArch()))
                .map(b -> {
                    String stripped = b.asBlobInfo().getName().replace(TAR_PREFIX, "");
                    return stripped.substring(0, stripped.indexOf("-"));
                }).sorted(VersioningUtils.SEMVER_COMPARATOR).collect(Collectors.toList());
        if (allRelevantVersions.isEmpty()) {
            throw new JenvtestException("Cannot find relevant version to download");
        }
        return allRelevantVersions.get(allRelevantVersions.size() - 1);
    }
}
