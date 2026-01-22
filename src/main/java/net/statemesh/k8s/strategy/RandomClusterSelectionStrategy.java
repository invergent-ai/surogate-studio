package net.statemesh.k8s.strategy;

import net.statemesh.k8s.util.ApiStub;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RandomClusterSelectionStrategy implements ClusterSelectionStrategy {
    @Override
    public String select(Map<String, ApiStub> clusters) {
        if (clusters == null || clusters.isEmpty()) {
            return null;
        }

        int rand = ThreadLocalRandom.current().nextInt(0, clusters.size());
        return clusters.keySet().stream().toList().get(rand);
    }
}
