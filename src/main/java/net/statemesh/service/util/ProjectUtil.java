package net.statemesh.service.util;

import net.statemesh.service.ProjectService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.ResourceDTO;

public class ProjectUtil {
    public static void updateProject(ProjectService projectService, ResourceDTO resource) {
        if (resource.getProject() != null && resource.getProject().getId() != null) {
            final ClusterDTO cluster = resource.getProject().getCluster();
            resource.setProject(
                projectService.findOne(resource.getProject().getId()).orElse(null)
            );
            if (resource.getProject() != null &&
                resource.getProject().getCluster() == null) {
                resource.getProject().setCluster(cluster);
            }
        }
    }
}
