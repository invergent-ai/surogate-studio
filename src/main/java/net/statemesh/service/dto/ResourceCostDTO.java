package net.statemesh.service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.Resource;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCostDTO {
    private String id;
    private String name;
    private String internalName;
    private String namespace;
    // Project related fields
    private String projectId;
    private String projectName;
    private String userId;
    private String userLogin;
    private String clusterId;
    private ResourceType resourceType;

    public static ResourceCostDTO fromApp(Resource resource) {
        return fromResource(resource, ResourceType.APPLICATION);
    }

    public static ResourceCostDTO fromDb(Resource resource) {
        return fromResource(resource, ResourceType.DATABASE);
    }

    public static ResourceCostDTO fromVm(Resource resource) {
        return fromResource(resource, ResourceType.VM);
    }

    private static ResourceCostDTO fromResource(Resource resource, ResourceType resourceType) {
        return ResourceCostDTO.builder()
            .id(resource.getId())
            .name(resource.getName())
            .internalName(resource.getInternalName())
            .namespace(
                !StringUtils.isEmpty(resource.getDeployedNamespace()) ?
                    resource.getDeployedNamespace() :
                    resource.getProject().getNamespace()
            )
            .projectId(resource.getProject().getId())
            .projectName(resource.getProject().getName())
            .userId(resource.getProject().getUser().getId())
            .userLogin(resource.getProject().getUser().getLogin())
            .clusterId(resource.getProject().getCluster() != null ?
                resource.getProject().getCluster().getId() : null)
            .resourceType(resourceType)
            .build();
    }

    public enum ResourceType {
        APPLICATION,
        DATABASE,
        VM
    }
}
