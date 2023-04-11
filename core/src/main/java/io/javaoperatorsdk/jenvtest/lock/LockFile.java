package io.javaoperatorsdk.jenvtest.lock;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;

public class LockFile {

  private static final Logger log = LoggerFactory.getLogger(LockFile.class);

  private final String dir;
  private final String lockFileName;

  public LockFile(String lockFileName, String dir) {
    this.dir = dir;
    this.lockFileName = lockFileName;
  }

  public boolean tryLock() {
    File file = new File(dir, lockFileName);
    try {
      return file.createNewFile();
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  public void releaseLock() {
    File file = new File(dir, lockFileName);
    try {
      Files.deleteIfExists(file.toPath());
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  public void waitUntilLockDeleted() {
    var file = new File(dir);
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
    } catch (IOException e) {
      throw new JenvtestException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }

}
