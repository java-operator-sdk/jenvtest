package com.csviri.kubeapi;

public class KubeApiException extends RuntimeException{

    public KubeApiException() {
    }

    public KubeApiException(String message) {
        super(message);
    }

    public KubeApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public KubeApiException(Throwable cause) {
        super(cause);
    }

    public KubeApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
