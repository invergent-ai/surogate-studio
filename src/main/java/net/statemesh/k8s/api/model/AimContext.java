package net.statemesh.k8s.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public class AimContext {
    private final Map<String, Object> values;

    public AimContext(Map<String, Object> values) {
        this.values = values != null ? values : new HashMap<>();
    }

    @JsonValue
    public Map<String, Object> asJson() {
        return values;
    }
}
