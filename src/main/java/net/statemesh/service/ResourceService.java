package net.statemesh.service;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.k8s.ResourceContext;
import net.statemesh.service.k8s.status.AppStatusService;
import net.statemesh.service.util.ProfileUtil;

import com.google.common.collect.Streams;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.NodeReservation;
import net.statemesh.domain.User;
import net.statemesh.domain.Volume;
import net.statemesh.domain.VolumeMount;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.domain.enumeration.VolumeType;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.repository.*;
import net.statemesh.service.dto.ResourceDTO;
import net.statemesh.service.exception.UserNotFoundException;
import net.statemesh.service.mapper.ApplicationMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.DatabaseMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {
    private static final String STATUS_DEPLOYED = "DEPLOYED";
    private static final List<Profile> EXCLUDED_PROFILES_FROM_FREE_TIER =
        List.of(Profile.HPC, Profile.GPU, Profile.MYNODE);

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final DatabaseRepository databaseRepository;
    private final DatabaseMapper databaseMapper;
    private final ApplicationProperties applicationProperties;
    private final VolumeMountRepository volumeMountRepository;
    private final NodeReservationRepository nodeReservationRepository;
    private final Environment environment;
    private final ApplicationContext applicationContext;

    @Builder
    private record Limits(double cpuLimit, double memLimit) {}

    // we consider a resource RUNNING for free tier accounting only when ALL its pod statuses are in RUNNING stage.
    private boolean resourceIsRunning(ResourceDTO res) throws Exception {
        var appStatusService = applicationContext.getBean(AppStatusService.class);
        if (res == null || !STATUS_DEPLOYED.equals(res.getStatusValue()))
            return false;
        Collection<ResourceStatus> statuses = switch (res) {
            case ApplicationDTO app -> appStatusService.getResourceStatus(ResourceContext.Context.builder().application(app).build());
            case DatabaseDTO db -> Collections.emptyList();
            default -> throw new IllegalStateException("Unexpected value: " + res);
        };
        if (statuses == null || statuses.isEmpty()) return false; // No pod info yet -> not running
        return statuses.stream()
            .allMatch(s -> s != null && ResourceStatus.ResourceStatusStage.RUNNING.equals(s.getStage()));
    }

    @Transactional(readOnly = true)
    public boolean userIsEligibleForFreeTier(String login, Optional<ResourceDTO> newResource) {
        if (ProfileUtil.isAppliance(environment)){
            return true;
        }

        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));


        if (newResource.isPresent()) {
            if (EXCLUDED_PROFILES_FROM_FREE_TIER.contains(newResource.get().getProject().getProfile())) {
                return Boolean.FALSE;
            }


            var userResources = findUserResources(login);
            newResource.ifPresent(userResources::add);
            for (var iter = userResources.iterator(); iter.hasNext(); ) {
                var res = iter.next();
                try {
                    if (!this.resourceIsRunning(res)) {
                        iter.remove(); // Resource is not running
                    }
                } catch (Exception e) {
                    log.error("Failed to determine resource status for resource {}", res.getId(), e);
                    iter.remove(); // Assume the resource is not running
                }
            }

            // Check max simultaneous resources
            Optional<Limits> usedResources = userResources.stream()
                    .map(ResourceDTO::resourceAllocations)
                    .flatMap(Set::stream)
                    .map(allocation ->
                        Limits.builder()
                            .cpuLimit(
                                Optional.ofNullable(allocation.getCpuLimit())
                                    .map(limit -> limit * allocation.getReplicas())
                                    .orElse(0d)
                            )
                            .memLimit(
                                Optional.ofNullable(allocation.getMemLimit())
                                    .map(Double::parseDouble)
                                    .map(limit -> limit * allocation.getReplicas())
                                    .orElse(0d)
                            )
                            .build()
                    )
                    .reduce((l1, l2) ->
                        Limits.builder()
                            .cpuLimit(l1.cpuLimit() + l2.cpuLimit())
                            .memLimit(l1.memLimit() + l2.memLimit())
                            .build()
                    );
        }

        return Boolean.TRUE;
    }

    public List<ResourceDTO> findUserResources(String login) {
        return Streams.concat(
                applicationRepository.findUserApplications(login).stream()
                    .map(app -> applicationMapper.toDto(app, new CycleAvoidingMappingContext()))
                    .map(app -> (ResourceDTO) app),
                databaseRepository.findUserDatabases(login).stream()
                    .map(db -> databaseMapper.toDto(db, new CycleAvoidingMappingContext()))
                    .map(db -> (ResourceDTO) db)
        ).toList();
    }

    public List<String> getUserNodes(ResourceDTO resourceDTO) {
        if (resourceDTO.getProject() == null || resourceDTO.getProject().getUser() == null ||
            !Profile.MYNODE.equals(resourceDTO.getProject().getProfile())) {
            return Collections.emptyList();
        }

        return nodeReservationRepository
            .findAllForUser(resourceDTO.getProject().getUser().getLogin()).stream()
            .filter(reservation -> reservation.getNode() != null)
            .map(NodeReservation::getUserKey)
            .toList();
    }

    public void detachVolumesInUse(Set<VolumeMount> volumeMounts, boolean keepVolumes) {
        List<VolumeMount> toDetach = volumeMounts.stream()
            .filter(volumeMount -> Objects.nonNull(volumeMount.getVolume()))
            .filter(volumeMount -> !VolumeType.HOST_PATH.equals(volumeMount.getVolume().getType()) && !VolumeType.SHM.equals(volumeMount.getVolume().getType()))
            .filter(volumeMount -> {
                List<VolumeMount> otherVolumeMounts = volumeMountRepository
                    .otherVolumeMounts(volumeMount.getVolume().getId(), volumeMount.getId());
                return !otherVolumeMounts.isEmpty() || keepVolumes; // User wants to keep data OR other resources use this volume
            }).toList();
        toDetach.forEach(volumeMount -> volumeMount.setVolume(dummyVolume()));
    }

    private Volume dummyVolume() { // Dummy volume to bypass NotNull validation
        return Volume.builder()
            .name(RandomStringUtils.secure().nextAlphanumeric(5))
            .type(VolumeType.TEMPORARY)
            .build();
    }
}
