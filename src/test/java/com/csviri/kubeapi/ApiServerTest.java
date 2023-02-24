package com.csviri.kubeapi;

import org.junit.jupiter.api.Test;

class ApiServerTest {

    @Test
    void testStartup() {
        var kubeApi = new APIServer();
        kubeApi.start();
        kubeApi.stop();
    }

}