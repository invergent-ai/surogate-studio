package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.enumeration.ProbeType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProbeDTO implements Serializable {
    String id;
    ProbeType type;
    Integer initialDelaySeconds;
    Integer periodSeconds;
    Integer failureThreshold;
    Integer successThreshold;
    Integer timeoutSeconds;
    Long terminationGracePeriodSeconds;

    String httpPath;
    Integer httpPort;

    String tcpHost;
    Integer tcpPort;

    List<String> execCommand;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProbeDTO dto)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, dto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
