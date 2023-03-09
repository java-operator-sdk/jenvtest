package com.csviri.jenvtest;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;

import static com.csviri.jenvtest.TestUtils.testConfigMap;
import static org.assertj.core.api.Assertions.assertThat;


class ApiServerTest {

    @Test
    void sanityTest() {
        var kubeApi = new APIServer();
        try {
            kubeApi.start();

            var client = new KubernetesClientBuilder().build();
            client.resource(testConfigMap()).createOrReplace();
            var cm = client.resource(testConfigMap()).get();

            assertThat(cm).isNotNull();
        } finally {
            kubeApi.stop();
        }
    }

}