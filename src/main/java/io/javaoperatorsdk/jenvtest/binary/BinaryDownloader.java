package io.javaoperatorsdk.jenvtest.binary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import io.javaoperatorsdk.jenvtest.Utils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;

public class BinaryDownloader {

  private static final Logger log = LoggerFactory.getLogger(BinaryDownloader.class);

  private static final String OBJECT_TAR_PREFIX = "kubebuilder-tools-";

  private final String jenvtestDir;
  private final BinaryRepo binaryRepo;
  private final OSInfo osInfoProvider;

  public BinaryDownloader(String jenvtestDir, OSInfo osInfoProvider) {
    this.jenvtestDir = jenvtestDir;
    this.osInfoProvider = osInfoProvider;
    this.binaryRepo = new BinaryRepo(osInfoProvider);

  }

  BinaryDownloader(String jenvtestDir, BinaryRepo binaryRepo, OSInfo osInfoProvider) {
    this.jenvtestDir = jenvtestDir;
    this.binaryRepo = binaryRepo;
    this.osInfoProvider = osInfoProvider;
  }

  public File download(String version) {
    log.info("Downloading binaries with version: {}", version);
    var tempFile = binaryRepo.downloadVersionToTempFile(version);
    File dir = createDirForBinaries(version);
    extractFiles(tempFile, dir);
    var deleted = tempFile.delete();
    if (!deleted) {
      log.warn("Unable to delete temp file: {}", tempFile.getPath());
    }
    return dir;
  }

  public File downloadLatest() {
    String latest = findLatestVersion();
    return download(latest);
  }

  private void extractFiles(File tempFile, File dir) {
    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
        new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))))) {
      var entry = tarIn.getNextTarEntry();
      while (entry != null) {
        if (!entry.isDirectory()) {
          File file = extractEntry(entry, dir, tarIn);
          if (!file.setExecutable(true)) {
            throw new JenvtestException("Cannot make the file executable: " + file.getPath());
          }
        }
        entry = tarIn.getNextTarEntry();
      }
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  private File extractEntry(TarArchiveEntry entry, File dir, TarArchiveInputStream tarIn)
      throws IOException {
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
    return targetFile;
  }

  private File createDirForBinaries(String version) {
    var dir = new File(jenvtestDir, BinaryManager.BINARY_LIST_DIR + File.separator
        + version + Utils.platformSuffix(osInfoProvider));
    if (!dir.mkdirs()) {
      throw new JenvtestException("Cannot created director: " + dir.getPath());
    }
    return dir;
  }

  public String findLatestVersion() {
    var objects = binaryRepo.listObjectNames();
    var allRelevantVersions = objects.filter(o -> o.contains(osInfoProvider.getOSName())
        && o.contains(osInfoProvider.getOSArch()))
        .map(o -> {
          String stripped = o.replace(OBJECT_TAR_PREFIX, "");
          String version = stripped.substring(0, stripped.indexOf("-"));
          if (version.startsWith("v")) {
            version = version.substring(1);
          }
          return version;
        }).sorted(Utils.SEMVER_COMPARATOR).collect(Collectors.toList());
    if (allRelevantVersions.isEmpty()) {
      throw new JenvtestException("Cannot find relevant version to download");
    }
    return allRelevantVersions.get(allRelevantVersions.size() - 1);
  }
}
