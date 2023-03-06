package com.csviri.kubeapi;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.List.of;

public class BinaryManager {

    public static final String ETCD_BINARY_NAME = "etcd";
    public static final String API_SERVER_BINARY_NAME = "kube-apiserver";
    public static final String KUBECTL_BINARY_NAME = "kubectl";
    public static final String BINARY_LIST_DIR = "k8s";

    private Binaries binaries;

    private String jenvtestDir;
    private String apiServerVersion;

    public BinaryManager(String jenvtestDir, String apiServerVersion) {
        this.jenvtestDir = jenvtestDir;
        this.apiServerVersion = apiServerVersion;
        init();
    }

    private void init() {
        File binaryDir = targetBinaryDir();

        this.binaries = new Binaries(new File(binaryDir, ETCD_BINARY_NAME),
                new File(binaryDir, API_SERVER_BINARY_NAME),
                new File(binaryDir, KUBECTL_BINARY_NAME));
        if (!binaries.getApiServer().exists()) {
            throw new KubeApiException("API Server binary not found at path:" + binaries.getApiServer().getPath());
        }
        if (!binaries.getKubectl().exists()) {
            throw new KubeApiException("Kubectl binary not found at path:" + binaries.getKubectl().getPath());
        }
        if (!binaries.getEtcd().exists()) {
            throw new KubeApiException("Etcd binary not found at path:" + binaries.getEtcd().getPath());
        }
    }

    public Binaries binaries() {
        return binaries;

    }

    public static class Binaries {
        private final File etcd;
        private final File apiServer;
        private final File kubectl;

        public Binaries(File etcd, File apiServer, File kubectl) {
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

    private File targetBinaryDir() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String platformSuffix = ("-" + os + "-" + arch).toLowerCase();
        if (apiServerVersion != null) {
            return new File(jenvtestDir, BINARY_LIST_DIR + File.separator + apiServerVersion + platformSuffix);
        }
        File binariesListDir = new File(jenvtestDir, BINARY_LIST_DIR);
        var dirVersionList = List.of(binariesListDir.list((dir, name) -> name.endsWith(platformSuffix)))
                .stream().map(s -> s.substring(0,s.indexOf(platformSuffix)))
                .collect(Collectors.toList());

        String latest = SemverUtil.getLatestVersion(dirVersionList) + platformSuffix;
        return new File(binariesListDir, latest);
    }


}
