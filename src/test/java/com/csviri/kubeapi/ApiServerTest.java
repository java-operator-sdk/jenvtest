package com.csviri.kubeapi;

import org.junit.jupiter.api.Test;

class ApiServerTest {

    @Test
    void testStartup() {
        var kubeApi = new APIServer(new APIServerConfig().setBinaryDirectory("/home/csviri/Downloads/kubeapi"));
        kubeApi.start();

        kubeApi.stop();
    }

}