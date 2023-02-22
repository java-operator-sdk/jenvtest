package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class APIServer {

    private static final Logger log = LoggerFactory.getLogger(APIServer.class);

    private final APIServerConfig config;
    private BinaryHandler binaryHandler;
    private Process etcdProcess;

    public APIServer() {
        this(new APIServerConfig());
    }

    public APIServer(APIServerConfig config) {
        this.config = config;
        this.binaryHandler = new BinaryHandler(config.getBinaryDirectory());
    }

    public void start() {
        startEtcd();
        startApiServer();
    }



    public void stop() {
        stopApiServer();
        stopEtcd();
    }

    private void startApiServer() {

// ./kube-apiserver --cert-dir .
// --etcd-servers http://0.0.0.0:2379
// --authorization-mode RBAC
// --service-account-issuer https://localhost
// --service-account-signing-key-file /home/csviri/Downloads/kubeapi/tempcerts/apiserver.key
// --service-account-key-file /home/csviri/Downloads/kubeapi/tempcerts/apiserver.key
// --service-account-issuer /home/csviri/Downloads/kubeapi/tempcerts/apiserver.cert
// --disable-admission-plugins ServiceAccount


   // system:masters for client group
   // https://stackoverflow.com/questions/21297139/how-do-you-sign-a-certificate-signing-request-with-your-certification-authority
    }

    private void stopApiServer() {

    }

    private void stopEtcd() {
        if (etcdProcess != null) {
            etcdProcess.destroy();
        }
        log.debug("etcd stopped");
    }

    private void startEtcd() {
        var etcdBinary = binaryHandler.binaries().getEtcd();
        try {
            // todo config ports
            etcdProcess = new ProcessBuilder(etcdBinary.getAbsolutePath(),
                    "--listen-client-urls=http://0.0.0.0:2379",
                    "--advertise-client-urls=http://0.0.0.0:2379").start();
            log.debug("etcd started");
        } catch (IOException e) {
            throw new KubeApiException(e);
        }
    }
}
