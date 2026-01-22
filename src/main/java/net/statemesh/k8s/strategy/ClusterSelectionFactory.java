package net.statemesh.k8s.strategy;

import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ResourceUtil;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.dto.NodeBenchmarkDTO;

public class ClusterSelectionFactory {

    public static ClusterSelectionStrategy selectForApplication(KubernetesController controller,
                                                                ApplicationDTO application) {
        if (application.getProject().getCluster() != null) {
            return new ReuseClusterSelectionStrategy(application.getProject().getCluster());
        }

        if (ResourceUtil.highAppCPURequirement(application)) {
            return new ResourceClusterSelectionStrategy(controller,
                ResourceClusterSelectionStrategy.Resource.CPU,
                ResourceClusterSelectionStrategy.Type.HIGH,
                ResourceClusterSelectionStrategy.ProfileInfo.builder()
                    .profile(application.getProject().getProfile())
                    .datacenter(application.getProject().getDatacenterName())
                    .userId(application.getProject().getUser().getId())
                    .build()
            );
        } else if (ResourceUtil.highAppMemoryRequirement(application)) {
            return new ResourceClusterSelectionStrategy(controller,
                ResourceClusterSelectionStrategy.Resource.MEMORY,
                ResourceClusterSelectionStrategy.Type.HIGH,
                ResourceClusterSelectionStrategy.ProfileInfo.builder()
                    .profile(application.getProject().getProfile())
                    .datacenter(application.getProject().getDatacenterName())
                    .userId(application.getProject().getUser().getId())
                    .build()
            );
        }

        return new RandomClusterSelectionStrategy();
    }

    public static ClusterSelectionStrategy selectForDatabase(KubernetesController controller,
                                                             DatabaseDTO database) {
        if (database.getProject().getCluster() != null) {
            return new ReuseClusterSelectionStrategy(database.getProject().getCluster());
        }

        if (ResourceUtil.highDatabaseCPURequirement(database)) {
            return new ResourceClusterSelectionStrategy(controller,
                ResourceClusterSelectionStrategy.Resource.CPU,
                ResourceClusterSelectionStrategy.Type.HIGH,
                ResourceClusterSelectionStrategy.ProfileInfo.builder()
                    .profile(database.getProject().getProfile())
                    .datacenter(database.getProject().getDatacenterName())
                    .userId(database.getProject().getUser().getId())
                    .build()
            );
        } else if (ResourceUtil.highDatabaseMemoryRequirement(database)) {
            return new ResourceClusterSelectionStrategy(controller,
                ResourceClusterSelectionStrategy.Resource.MEMORY,
                ResourceClusterSelectionStrategy.Type.HIGH,
                ResourceClusterSelectionStrategy.ProfileInfo.builder()
                    .profile(database.getProject().getProfile())
                    .datacenter(database.getProject().getDatacenterName())
                    .userId(database.getProject().getUser().getId())
                    .build()
            );
        }

        return new RandomClusterSelectionStrategy();
    }

    public static ClusterSelectionStrategy selectForOnboarding(KubernetesController controller,
                                                               NodeBenchmarkDTO nodeBenchmarkDTO) {
        return ResourceUtil.highCPUResources(nodeBenchmarkDTO) ?
            new ResourceClusterSelectionStrategy(controller,
                ResourceClusterSelectionStrategy.Resource.CPU,
                ResourceClusterSelectionStrategy.Type.LOW,
                null
            ) :
            new ResourceClusterSelectionStrategy(controller,
                ResourceClusterSelectionStrategy.Resource.MEMORY,
                ResourceClusterSelectionStrategy.Type.LOW,
                null
            );
    }

    public static ClusterSelectionStrategy selectForPipelineRun() {
        return new RandomClusterSelectionStrategy();
    }

    public static ClusterSelectionStrategy selectForRayJob() {
        return new RandomClusterSelectionStrategy();
    }

    public static ClusterSelectionStrategy selectForTaskRun() {
        return new RandomClusterSelectionStrategy();
    }
}
