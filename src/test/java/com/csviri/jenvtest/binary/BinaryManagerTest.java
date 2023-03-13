package com.csviri.jenvtest.binary;

import com.csviri.jenvtest.APIServerConfig;
import com.csviri.jenvtest.APIServerConfigBuilder;
import com.csviri.jenvtest.JenvtestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BinaryManagerTest {

    @Test
    void throwsExceptionIfBinaryNotPresentAndInOfflineMode() {
        BinaryManager binaryManager = new BinaryManager(APIServerConfigBuilder.anAPIServerConfig()
                .withDownloadBinaries(false)
                .withApiServerVersion("1.0.1")
                .build());

        assertThrows(JenvtestException.class, binaryManager::initAndDownloadIfRequired);
    }

}