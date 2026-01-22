package net.statemesh.k8s.util;

import io.kubernetes.client.openapi.models.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import net.statemesh.service.dto.ApplicationDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Getter
@ToString
@Builder
public class ResourceStatus {
    private ApplicationDTO app;
    private String podName;
    private String component;
    private ResourceStatusStage stage;
    private String message;
    private List<String> details;
    private List<ContainerStatus> containerStatuses;

    public static List<ResourceStatus> fromStoppedDeployment(ApplicationDTO application, String component) {
        return application.getContainers().stream().map(container -> {
            String podName = RandomStringUtils.insecure().nextAlphanumeric(10);
            return ResourceStatus.builder()
                .app(application)
                .podName(podName)
                .component(component)
                .stage(ResourceStatusStage.STOPPED)
                .containerStatuses(Collections.emptyList())
                .build();
        }).toList();
    }

    public static ResourceStatus fromPodStatus(ApplicationDTO app, V1PodStatus status, String podName) {
        return fromPodStatus(app, status, podName, null);
    }

    public static List<ResourceStatus> fromStoppedStatefulSet(ApplicationDTO application, String component) {
        return List.of(
            ResourceStatus.builder()
                .app(application)
                .podName(application.getInternalName() + "-0")
                .component(component)
                .stage(ResourceStatusStage.STOPPED)
                .message("StatefulSet is stopped (0 replicas)")
                .details(List.of("The StatefulSet has been scaled to 0 replicas"))
                .containerStatuses(Collections.emptyList())
                .build()
        );
    }

    public static ResourceStatus fromPodStatus(ApplicationDTO app, V1PodStatus status, String podName, String component) {
        if (status == null || status.getPhase() == null) {
            return ResourceStatus.builder().stage(ResourceStatusStage.UNKNOWN).build();
        }

        String phase = status.getPhase();

        boolean ready = false;
        boolean containersReady = false;
        boolean scheduled = false;
        List<V1PodCondition> conditions = status.getConditions();
        if (conditions != null) {
            for (V1PodCondition c : conditions) {
                if (c == null) continue;
                boolean isTrue = "True".equalsIgnoreCase(c.getStatus());
                switch (c.getType()) {
                    case "Ready" -> ready = isTrue;
                    case "ContainersReady" -> containersReady = isTrue;
                    case "PodScheduled" -> scheduled = isTrue;
                    default -> {
                    }
                }
            }
        }

        List<String> waitingReasons = new ArrayList<>();
        List<String> terminationReasons = new ArrayList<>();

        ResourceStatusStage earlyStage = null;
        List<V1ContainerStatus> initStatuses = status.getInitContainerStatuses();
        List<ContainerStatus> builtContainerStatuses = new ArrayList<>();
        if (initStatuses != null) {
            for (V1ContainerStatus init : initStatuses) {
                if (init.getState() == null) continue;
                V1ContainerState st = init.getState();
                if (st.getWaiting() != null) {
                    // Capture waiting reason (or message if reason blank) for init containers
                    String reason = st.getWaiting().getReason();
                    String msg = st.getWaiting().getMessage();
                    if (StringUtils.isNotBlank(reason)) waitingReasons.add(reason);
                    else if (StringUtils.isNotBlank(msg)) waitingReasons.add(msg);
                    // Waiting init container: decide severity
                    if ("CrashLoopBackOff".equalsIgnoreCase(reason)) {
                        earlyStage = ResourceStatusStage.RESTARTING;
                    } else if ("ImagePullBackOff".equalsIgnoreCase(reason) || "ErrImagePull".equalsIgnoreCase(reason)) {
                        earlyStage = ResourceStatusStage.FAILED; // treat image pull problems as failed early
                    } else {
                        if (earlyStage == null) earlyStage = ResourceStatusStage.INITIALIZING;
                    }
                    break; // still in init sequence; later app containers not started yet
                }
                if (st.getRunning() != null) {
                    // An init container currently running => still initializing, but do NOT downgrade a prior FAILED/RESTARTING
                    if (earlyStage == null) {
                        earlyStage = ResourceStatusStage.INITIALIZING;
                    }
                }
                if (st.getTerminated() != null) {
                    Integer exit = st.getTerminated().getExitCode();
                    if (exit != 0) {
                        // Failed init container always means pod is effectively FAILED (phase may still be Pending)
                        String reason = st.getTerminated().getReason();
                        String msg = st.getTerminated().getMessage();
                        if (StringUtils.isNotBlank(reason)) terminationReasons.add(reason);
                        else if (StringUtils.isNotBlank(msg))
                            terminationReasons.add(msg); // ensure we retain some diagnostic detail
                        earlyStage = ResourceStatusStage.FAILED;
                        break; // no need to inspect further init containers
                    }
                }
            }
            // Include init container status objects
            for (V1ContainerStatus init : initStatuses) {
                builtContainerStatuses.add(ContainerStatus.fromStatus(app, init, true));
            }
        }

        List<V1ContainerStatus> containers = status.getContainerStatuses();
        int running = 0;
        int waiting = 0;
        int normalTerminated = 0;
        int abnormalTerminated = 0;
        int restarting = 0;
        if (containers != null) {
            for (V1ContainerStatus cs : containers) {
                V1ContainerState current = cs.getState();
                if (current != null) {
                    if (current.getRunning() != null) {
                        running++;
                    } else if (current.getWaiting() != null) {
                        waiting++;
                        String reason = current.getWaiting().getReason();
                        String msg = current.getWaiting().getMessage();
                        if (StringUtils.isNotBlank(reason)) {
                            waitingReasons.add(reason);
                            if ("CrashLoopBackOff".equalsIgnoreCase(reason)) restarting++;
                        } else if (StringUtils.isNotBlank(msg)) {
                            waitingReasons.add(msg);
                        }
                    } else if (current.getTerminated() != null) {
                        V1ContainerStateTerminated term = current.getTerminated();
                        Integer exit = term.getExitCode();
                        if (exit == 0) {
                            normalTerminated++;
                        } else {
                            abnormalTerminated++;
                            String reason = term.getReason();
                            String msg = term.getMessage();
                            if (StringUtils.isNotBlank(reason)) terminationReasons.add(reason);
                            else if (StringUtils.isNotBlank(msg)) terminationReasons.add(msg);
                        }
                    }
                }
                builtContainerStatuses.add(ContainerStatus.fromStatus(app, cs, false));
            }
        }

        if (earlyStage != null) {
            waitingReasons = distinct(waitingReasons);
            String msg = !waitingReasons.isEmpty() ? waitingReasons.getFirst() : null;
            boolean earlyImagePullError = waitingReasons.stream()
                .anyMatch(r -> "ImagePullBackOff".equalsIgnoreCase(r) || "ErrImagePull".equalsIgnoreCase(r));
            if (earlyStage == ResourceStatusStage.FAILED) {
                if (!terminationReasons.isEmpty()) {
                    msg = terminationReasons.getFirst();
                } else if (earlyImagePullError) {
                    msg = "Image pull error";
                } else if (msg == null) {
                    if (StringUtils.isNotBlank(status.getReason())) msg = status.getReason();
                    else if (StringUtils.isNotBlank(status.getMessage())) msg = status.getMessage();
                }
            } else if (earlyStage == ResourceStatusStage.INITIALIZING && msg == null) {
                // Provide a minimal hint instead of null when we know we are initializing
                msg = "Initializing";
            }
            List<String> details = mergeDetails(waitingReasons, terminationReasons);
            return ResourceStatus.builder()
                .stage(earlyStage)
                .message(msg)
                .component(component)
                .details(details)
                .containerStatuses(builtContainerStatuses.isEmpty() ? null : builtContainerStatuses)
                .build();
        }

        ResourceStatusStage resolvedStage;
        if ("Succeeded".equalsIgnoreCase(phase)) {
            resolvedStage = ResourceStatusStage.COMPLETED;
        } else if ("Failed".equalsIgnoreCase(phase)) {
            resolvedStage = ResourceStatusStage.FAILED;
        } else if ("Unknown".equalsIgnoreCase(phase)) {
            resolvedStage = ResourceStatusStage.UNKNOWN;
        } else if ("Pending".equalsIgnoreCase(phase)) {
            if (!scheduled) {
                resolvedStage = ResourceStatusStage.WAITING;
            } else if ((containers == null || containers.isEmpty()) && (initStatuses == null || initStatuses.isEmpty())) {
                resolvedStage = ResourceStatusStage.WAITING;
            } else {
                resolvedStage = ResourceStatusStage.INITIALIZING;
            }
        } else if ("Running".equalsIgnoreCase(phase)) {
            // Note: A pod with all containers terminated normally should have phase Succeeded; this branch is defensive.
            if (running == 0 && waiting == 0 && abnormalTerminated == 0 && normalTerminated > 0) {
                resolvedStage = ResourceStatusStage.COMPLETED;
            } else if (running == 0 && abnormalTerminated > 0) {
                resolvedStage = ResourceStatusStage.FAILED;
            } else if (restarting > 0) {
                resolvedStage = running > 0 ? ResourceStatusStage.DEGRADED : ResourceStatusStage.RESTARTING;
            } else if (running > 0 && (waiting > 0 || abnormalTerminated > 0)) {
                resolvedStage = ResourceStatusStage.DEGRADED;
            } else if (running == 0 && waiting > 0) {
                resolvedStage = ResourceStatusStage.WAITING;
            } else if (running > 0 && waiting == 0 && abnormalTerminated == 0) {
                resolvedStage = (ready && containersReady) ? ResourceStatusStage.RUNNING : ResourceStatusStage.WAITING;
            } else {
                resolvedStage = ResourceStatusStage.DEGRADED;
            }
        } else {
            resolvedStage = ResourceStatusStage.UNKNOWN;
        }

        waitingReasons = distinct(waitingReasons);
        boolean hasImagePullError = waitingReasons.stream().anyMatch(r -> "ImagePullBackOff".equalsIgnoreCase(r) || "ErrImagePull".equalsIgnoreCase(r));
        // Also escalate RESTARTING when an image pull error is present across any container (higher severity than crash loop)
        if (hasImagePullError && (resolvedStage == ResourceStatusStage.WAITING || resolvedStage == ResourceStatusStage.INITIALIZING || resolvedStage == ResourceStatusStage.DEGRADED || resolvedStage == ResourceStatusStage.RESTARTING)) {
            resolvedStage = ResourceStatusStage.FAILED;
        }

        String message = null;
        if ((resolvedStage == ResourceStatusStage.WAITING || resolvedStage == ResourceStatusStage.INITIALIZING || resolvedStage == ResourceStatusStage.RESTARTING || resolvedStage == ResourceStatusStage.DEGRADED) && !waitingReasons.isEmpty()) {
            if (waitingReasons.stream().anyMatch("CrashLoopBackOff"::equalsIgnoreCase)) {
                message = "CrashLoopBackOff";
            } else {
                message = waitingReasons.getFirst();
            }
        } else if (resolvedStage == ResourceStatusStage.FAILED) {
            if (hasImagePullError) {
                message = "Image pull error";
            } else if (!terminationReasons.isEmpty()) {
                message = terminationReasons.getFirst();
            } else if (abnormalTerminated > 0) {
                message = "Abnormal container termination";
            } else if (StringUtils.isNotBlank(status.getReason())) {
                message = status.getReason();
            } else if (StringUtils.isNotBlank(status.getMessage())) {
                message = status.getMessage();
            }
        }
        // Fallback for DEGRADED: if no waitingReasons but we have termination reasons, surface them
        if (message == null && resolvedStage == ResourceStatusStage.DEGRADED) {
            if (!terminationReasons.isEmpty()) {
                message = terminationReasons.getFirst();
            } else if (abnormalTerminated > 0) {
                message = "Abnormal container termination";
            }
        }

        List<String> details = mergeDetails(waitingReasons, terminationReasons);

        return ResourceStatus.builder()
            .podName(podName)
            .component(component)
            .stage(resolvedStage)
            .message(message)
            .details(details)
            .containerStatuses(builtContainerStatuses.isEmpty() ? null : builtContainerStatuses)
            .build();
    }

    private static List<String> mergeDetails(List<String> waitingReasons, List<String> terminationReasons) {
        List<String> merged = new ArrayList<>();
        if (waitingReasons != null) merged.addAll(waitingReasons);
        if (terminationReasons != null) {
            for (String tr : terminationReasons) {
                if (tr != null && merged.stream().noneMatch(d -> d.equalsIgnoreCase(tr))) {
                    merged.add(tr);
                }
            }
        }
        return merged.isEmpty() ? null : merged;
    }

    private static List<String> distinct(List<String> original) {
        if (original == null || original.isEmpty()) return original;
        Set<String> set = new LinkedHashSet<>();
        for (String s : original) {
            if (StringUtils.isNotBlank(s)) set.add(s.trim());
        }
        return new ArrayList<>(set);
    }

    public ResourceStatus message(String message) {
        this.message = message;
        return this;
    }

    public ResourceStatus details(List<String> details) {
        this.details = details;
        return this;
    }

    @Getter
    @Builder
    public static class ContainerStatus {
        private String containerId;
        private String containerName;
        private boolean init;
        private ContainerStatusStage stage;
        private ContainerStatusStage lastStage;
        private String waitingMessage;
        private String lastStageTerminatedMessage;
        private String lastStageTerminatedReason;

        public static ContainerStatus fromStatus(ApplicationDTO app, V1ContainerStatus status, boolean init) {
            if (status == null) return null;
            V1ContainerState current = status.getState();
            ContainerStatusStage stage = null;
            String waitingMsg = null;
            if (current != null) {
                stage = current.getRunning() != null ? ContainerStatusStage.RUNNING :
                    current.getWaiting() != null ? ContainerStatusStage.WAITING :
                        current.getTerminated() != null ? ContainerStatusStage.TERMINATED : null;
                if (current.getWaiting() != null) {
                    String reason = current.getWaiting().getReason();
                    String msg = current.getWaiting().getMessage();
                    if (StringUtils.isNotBlank(reason) && StringUtils.isNotBlank(msg)) {
                        waitingMsg = reason + ": " + msg;
                    } else if (StringUtils.isNotBlank(reason)) {
                        waitingMsg = reason;
                    } else if (StringUtils.isNotBlank(msg)) {
                        waitingMsg = msg;
                    }
                }
            }
            ContainerStatusStage lastStage = null;
            String lastTermMsg = null;
            String lastTermReason = null;
            if (status.getLastState() != null) {
                V1ContainerState last = status.getLastState();
                lastStage = last.getRunning() != null ? ContainerStatusStage.RUNNING :
                    last.getWaiting() != null ? ContainerStatusStage.WAITING :
                        last.getTerminated() != null ? ContainerStatusStage.TERMINATED : null;
                if (last.getTerminated() != null) {
                    lastTermMsg = last.getTerminated().getMessage();
                    lastTermReason = last.getTerminated().getReason();
                }
            }

            var containerStatusBuilder = ContainerStatus.builder()
                .containerName(status.getName())
                .init(init)
                .stage(stage)
                .waitingMessage(waitingMsg)
                .lastStage(lastStage)
                .lastStageTerminatedMessage(lastTermMsg)
                .lastStageTerminatedReason(lastTermReason);

            // Try to match container with application's containers
            // For databases (PostgreSQL), app.getContainers() might be null or empty
            if (app != null && app.getContainers() != null && !app.getContainers().isEmpty()) {
                var appContainers = app.getContainers().stream()
                    .filter(c -> status.getName().startsWith(NamingUtils.containerName(app.getInternalName(), c.getImageName())))
                    .toList();

                if (appContainers.size() > 1) {
                    throw new IllegalStateException("Multiple app containers found for " + status.getName());
                }

                // Only set containerId if we found a matching container
                if (!appContainers.isEmpty()) {
                    containerStatusBuilder.containerId(appContainers.getFirst().getId());
                }
            }

            return containerStatusBuilder.build();
        }
    }

    public enum ResourceStatusStage {
        INITIALIZING, // init containers still running / waiting
        RUNNING, // all expected containers running & ready
        WAITING, // scheduled but not ready (pulling images / readiness not passed)
        RESTARTING, // crash loop without any healthy running containers
        DEGRADED, // some containers running but others waiting / restarting / failed
        COMPLETED, // all containers terminated successfully (phase Succeeded)
        FAILED, // one or more containers terminated abnormally (phase Failed or crash with no healthy containers)
        STOPPED, // deployment has zero replicas
        UNKNOWN, // insufficient or unknown information
    }

    public enum ContainerStatusStage {
        RUNNING,
        TERMINATED,
        WAITING
    }
}
