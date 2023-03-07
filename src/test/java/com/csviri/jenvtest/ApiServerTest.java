package com.csviri.jenvtest;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class ApiServerTest {

    @Test
    void sanityTest() {
        var kubeApi = new APIServer();
        try {
            kubeApi.start();

            var client = new KubernetesClientBuilder().build();
            client.resource(configMap()).createOrReplace();
            var cm = client.resource(configMap()).get();

            assertThat(cm).isNotNull();
        } finally {
            kubeApi.stop();
        }
    }

    private ConfigMap configMap() {
        return new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("test1")
                        .withNamespace("default")
                        .build())
                .withData(Map.of("key","data"))
                .build();
    }

}