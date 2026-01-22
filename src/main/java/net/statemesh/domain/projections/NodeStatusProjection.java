package net.statemesh.domain.projections;

import net.statemesh.domain.enumeration.NodeStatus;

import java.time.Instant;

public interface NodeStatusProjection {
    NodeStatus getStatus();
    Instant getLastStartTime();
}
