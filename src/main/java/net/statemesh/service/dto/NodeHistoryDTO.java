package net.statemesh.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.statemesh.domain.enumeration.NodeActionType;
import net.statemesh.domain.enumeration.NodeResourceType;
import net.statemesh.domain.enumeration.NodeUpdateType;

import java.io.Serializable;
import java.time.Instant;


@Getter
@Setter
@ToString(exclude = {"newYaml"})
@Builder
public class NodeHistoryDTO implements Serializable {
    private String id;

    @NotNull
    private Instant timeStamp;

    @NotNull
    private NodeActionType action;

    private NodeUpdateType updateType;
    private NodeResourceType resourceType;

    @Lob
    private String newYaml;
}
