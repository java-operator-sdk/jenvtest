package com.csviri.jenvtest;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CertManagerTest {

    File certsDir = new File("target","certs");
    CertManager certManager = new CertManager(certsDir.getPath());

    @AfterEach
    void cleanup() throws IOException {
        if (certsDir.exists()) {
            FileUtils.cleanDirectory(certsDir);
        }
    }

    @Test
    void generatesCertificates() throws IOException {
        certsDir.mkdirs();
        FileUtils.cleanDirectory(certsDir);

        certManager.createCertificatesIfNeeded();

        var files = List.of(certsDir.list());
        assertThat(files.size()).isEqualTo(4);
    }

}