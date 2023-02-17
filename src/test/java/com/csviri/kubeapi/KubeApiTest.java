package com.csviri.kubeapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KubeApiTest {

    @Test
    void testStartup() {
        var kubeApi = new KubeApi(new KubeApiConfig().setBinaryDirectory("/home/csviri/Downloads/kubeapi"));
        kubeApi.start();

        kubeApi.stop();
    }

}