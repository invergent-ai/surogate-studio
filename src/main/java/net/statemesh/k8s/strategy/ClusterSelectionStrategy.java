package net.statemesh.k8s.strategy;

import net.statemesh.k8s.util.ApiStub;

import java.util.Map;

public interface ClusterSelectionStrategy {
    String select(Map<String, ApiStub> clusters);
}
