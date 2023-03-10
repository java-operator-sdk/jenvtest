package com.csviri.jenvtest.junit;

import com.csviri.jenvtest.TestUtils;
import com.csviri.jenvtest.junit.EnableAPIServer;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.csviri.jenvtest.TestUtils.testConfigMap;
import static org.assertj.core.api.Assertions.assertThat;

@EnableAPIServer
class JUnitExtensionTest {

    @Test
    void testCommunication() {
        var client = new KubernetesClientBuilder().build();
        client.resource(testConfigMap()).createOrReplace();
        var cm = client.resource(TestUtils.testConfigMap()).get();

        assertThat(cm).isNotNull();
    }
}
