package net.statemesh.k8s.api.model;

import lombok.Data;

import java.util.List;

@Data
public class AimMetric {
    private String name;
    private List<Double> iters;
    private List<Double> values;
}
