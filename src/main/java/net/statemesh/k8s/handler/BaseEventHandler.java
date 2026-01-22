package net.statemesh.k8s.handler;

import net.statemesh.service.dto.ClusterDTO;

public abstract class BaseEventHandler {
    private final String zoneId;
    private final ClusterDTO cluster;

    public BaseEventHandler(String zoneId, ClusterDTO cluster) {
        this.zoneId = zoneId;
        this.cluster = cluster;
    }

    protected String getZoneId() {
        return zoneId;
    }

    protected ClusterDTO getCluster() {
        return cluster;
    }
}
