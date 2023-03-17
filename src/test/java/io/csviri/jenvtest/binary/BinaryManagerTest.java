package io.csviri.jenvtest.binary;

import org.junit.jupiter.api.Test;

import io.csviri.jenvtest.JenvtestException;
import io.csviri.jenvtest.KubeAPIServerConfigBuilder;

import static org.junit.jupiter.api.Assertions.*;

class BinaryManagerTest {

  @Test
  void throwsExceptionIfBinaryNotPresentAndInOfflineMode() {
    BinaryManager binaryManager = new BinaryManager(KubeAPIServerConfigBuilder.anAPIServerConfig()
        .withDownloadBinaries(false)
        .withApiServerVersion("1.0.1")
        .build());

    assertThrows(JenvtestException.class, binaryManager::initAndDownloadIfRequired);
  }

}
