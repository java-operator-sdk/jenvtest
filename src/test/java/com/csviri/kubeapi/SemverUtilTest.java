package com.csviri.kubeapi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemverUtilTest {

    @Test
    void getsLatestVersion() {
        assertThat(SemverUtil.getLatestVersion(new ArrayList<>(List.of("1.22.4","1.26.3","1.26.1","1.11.2"))))
                .isEqualTo("1.26.3");
    }
}