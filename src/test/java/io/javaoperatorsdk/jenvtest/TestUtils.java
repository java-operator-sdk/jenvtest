package io.javaoperatorsdk.jenvtest;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class TestUtils {

  public static ConfigMap testConfigMap() {
    return new ConfigMapBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("test1")
            .withNamespace("default")
            .build())
        .withData(Map.of("key", "data"))
        .build();
  }

}
