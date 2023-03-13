package com.csviri.jenvtest.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ExtendWith(APIServerExtension.class)
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface EnableAPIServer {

}
