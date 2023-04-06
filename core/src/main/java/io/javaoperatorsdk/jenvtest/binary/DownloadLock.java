package io.javaoperatorsdk.jenvtest.binary;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;

public class DownloadLock {

  private static final Logger log = LoggerFactory.getLogger(DownloadLock.class);

  public static final String LOCK_SUFFIX = ".lock";
  private final String downloadDir;
  private final String lockFileName;

  public DownloadLock(String version, String downloadDir) {
    this.downloadDir = downloadDir;
    this.lockFileName = version + LOCK_SUFFIX;
  }

  public boolean tryLock() {
    File file = new File(downloadDir, lockFileName);
    try {
      return file.createNewFile();
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  public void releaseLock() {
    File file = new File(downloadDir, lockFileName);
    var deleted = file.delete();
    if (!deleted) {
      throw new JenvtestException("Lock file not deleted: " + file.getPath());
    }
  }

  public void waitUntilLockDeleted() {
    var file = new File(downloadDir);
    var path = file.toPath();

    try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
      path.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
      while (true) {
        final WatchKey wk = watchService.take();
        for (WatchEvent<?> event : wk.pollEvents()) {
          final Path changed = (Path) event.context();
          log.info("!! Event path: {}  event: {}", changed, event);
          if (changed.endsWith(lockFileName)) {
            return;
          }
        }
        // reset the key
        boolean valid = wk.reset();
        if (!valid) {
          log.warn("Watch key no longer valid");
        }
      }
    } catch (IOException | InterruptedException e) {
      throw new JenvtestException(e);
    }
  }

}
