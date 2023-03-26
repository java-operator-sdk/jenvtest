package io.javaoperatorsdk.jenvtest.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ExtendWith(KubeAPIServerExtension.class)
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface EnableKubeAPIServer {

  String NOT_SET = "NOT_SET";

  /**
   * The target Kube API Version. Sample: 1.26.1
   */
  String kubeAPIVersion() default NOT_SET;

  String[] apiServerFlags() default {};
}
