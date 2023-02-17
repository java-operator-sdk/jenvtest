package com.csviri.kubeapi;

import java.io.File;

public class BinaryHandler {
    public static final String DEFAULT_VERSION_1_26_1 = "v1.26.1";
    public static final String ETCD_BINARY_NAME = "etcd";
    public static final String API_SERVER_BINARY_NAME = "kube-apiserver";


    private String downloadDirectory;
    // todo configurable
    private String version = DEFAULT_VERSION_1_26_1;

    //    private String url ="https://dl.k8s.io/v1.26.1/bin/linux/amd64/kube-apiserver";

    public BinaryHandler(String downloadDirectory) {
        this.downloadDirectory = downloadDirectory;
    }

    public ApiBinaries downloadBinaries() {
        return null;
    }

    public ApiBinaries binaries() {
        return new ApiBinaries(new File(downloadDirectory, ETCD_BINARY_NAME),
                new File(downloadDirectory, API_SERVER_BINARY_NAME));
    }


    public static class ApiBinaries {
        private final File etcd;
        private final File apiServer;

        public ApiBinaries(File etcd, File apiServer) {
            this.etcd = etcd;
            this.apiServer = apiServer;
        }

        public File getEtcd() {
            return etcd;
        }

        public File getApiServer() {
            return apiServer;
        }
    }
}
