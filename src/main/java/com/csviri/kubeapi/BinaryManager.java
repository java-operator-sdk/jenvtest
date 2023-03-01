package com.csviri.kubeapi;

import java.io.File;

public class BinaryManager {
    public static final String DEFAULT_VERSION_1_26_1 = "v1.26.1";
    public static final String ETCD_BINARY_NAME = "etcd";
    public static final String API_SERVER_BINARY_NAME = "kube-apiserver";
    public static final String KUBECTL_BINARY_NAME = "kubectl";

    // todo configurable
    //https://dl.k8s.io/v1.26.1/bin/linux/amd64/kube-apiserver
    private String version = DEFAULT_VERSION_1_26_1;

    private String jenvtestDir;

    public BinaryManager(String jenvtestDir) {
        this.jenvtestDir = jenvtestDir;
    }

    public ApiBinaries downloadBinariesIfNotPresent() {


        return null;
    }

    public ApiBinaries binaries() {
        return new ApiBinaries(new File(jenvtestDir, ETCD_BINARY_NAME),
                new File(jenvtestDir, API_SERVER_BINARY_NAME),
                new File(jenvtestDir, KUBECTL_BINARY_NAME));
    }

    public static class ApiBinaries {
        private final File etcd;
        private final File apiServer;
        private final File kubectl;

        public ApiBinaries(File etcd, File apiServer, File kubectl) {
            this.etcd = etcd;
            this.apiServer = apiServer;
            this.kubectl = kubectl;
        }

        public File getEtcd() {
            return etcd;
        }

        public File getApiServer() {
            return apiServer;
        }

        public File getKubectl() {
            return kubectl;
        }
    }
}
