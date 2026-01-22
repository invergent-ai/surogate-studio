package net.statemesh.k8s.strategy;

import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.ClusterDTO;

import java.util.Map;

public class ReuseClusterSelectionStrategy implements ClusterSelectionStrategy {
    private final ClusterDTO cluster;

    public ReuseClusterSelectionStrategy(ClusterDTO cluster) {
        this.cluster = cluster;
    }

    @Override
    public String select(Map<String, ApiStub> clusters) {
        if (!clusters.containsKey(cluster.getCid())) {
            throw new RuntimeException("Cluster to reuse was not present");
        }
        return cluster.getCid();
    }
}
