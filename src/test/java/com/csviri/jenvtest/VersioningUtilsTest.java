package com.csviri.jenvtest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VersioningUtilsTest {

    @Test
    void getsLatestVersion() {
        assertThat(VersioningUtils.getLatestVersion(new ArrayList<>(List.of("1.22.4","1.26.3","1.26.1","1.11.2"))))
                .isEqualTo("1.26.3");
    }
}