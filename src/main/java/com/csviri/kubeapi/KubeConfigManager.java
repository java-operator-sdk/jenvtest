package com.csviri.kubeapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubeConfigManager {

    private static final Logger log = LoggerFactory.getLogger(KubeConfigManager.class);

    public void updateKubeConfig() {
        log.debug("Adding settings to kubeconfig");
        addUser();
        addCluster();
        addContext();
        setDefaultContext();
    }

    public void removeFromKubeConfig() {
        log.debug("Removing settings from kubeconfig");
    }

    private void addUser() {

    }

    private void addCluster() {

    }

    private void addContext() {

    }

    private void setDefaultContext() {

    }

}
