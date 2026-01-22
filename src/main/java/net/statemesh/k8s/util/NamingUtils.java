package net.statemesh.k8s.util;

import net.statemesh.domain.enumeration.RayJobType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.statemesh.k8s.util.K8SConstants.*;

public class NamingUtils {
    public static String containerName(String applicationName, String imageName) {
        return rfc1123Name(
            StringUtils.join(
                applicationName,
                "-",
                rfc1123Name(imageName))
        );
    }

    public static String serviceName(String applicationName, String portName) {
        return rfc1123Name(
            StringUtils.join(
                portName(applicationName, portName),
                "-Service"
            )
        );
    }

    public static String ingressName(String applicationName, String portName) {
        return rfc1123Name(
            StringUtils.join(
                portName(applicationName, portName),
                "-IngressRoute"
            )
        );
    }

    public static String ipAllowMiddlewareName(String applicationName, String portName) {
        return rfc1123Name(
            StringUtils.join(
                portName(applicationName, portName),
                "-IPA-Middleware"
            )
        );
    }

    public static String volumeName(String volumeName) {
        return rfc1123Name(volumeName);
    }

    public static String folderName(String volumeName) {
        return rfc1123Name(
            StringUtils.join(
                volumeName(volumeName),
                "-",
                RandomStringUtils.secure().nextAlphanumeric(5).toLowerCase()
            )
        );
    }

    public static String storageSecretName(String volumeName, boolean privateStorage) {
        return rfc1123Name(
            StringUtils.join(
                "juicefs-",
                privateStorage ? "priv-" : "",
                volumeName(volumeName)
            )
        );
    }

    public static String storageClassName(String volumeName, boolean privateStorage) {
        return rfc1123Name(
            StringUtils.join(
                "juicefs-sc-",
                privateStorage ? "priv-" : "",
                volumeName(volumeName)
            )
        );
    }

    public static String pvcName(String volumeName) {
        return rfc1123Name(
            StringUtils.join(
                "data-",
                volumeName(volumeName)
            )
        );
    }

    public static String publicHostname(String clusterId, String applicationName, String prefix, String webDomain) {
        return StringUtils.join(
            prefix,
            rfc1123Name(applicationName),
            "-",
            RandomStringUtils.secure().nextAlphanumeric(5).toLowerCase(),
            ".",
            clusterId,
            webDomain
        );
    }

    public static String resourceName(String resourceName) {
        return rfc1123Name(
            StringUtils.join(
                resourceName,
                "-",
                RandomStringUtils.secure().nextAlphanumeric(5).toLowerCase()
            )
        );
    }

    public static String workDirVolumeName(String volumeName) {
        return rfc1123Name(
            StringUtils.join(
                volumeName,
                "-",
                RandomStringUtils.secure().nextAlphanumeric(5).toLowerCase()
            )
        );
    }

    public static String rfc1123Name(String name) {
        return StringUtils.left(
            name
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")  // Replace invalid chars with hyphen
                .replaceAll("-+", "-")          // Replace multiple hyphens with single hyphen
                .replaceAll("^-", ""),          // Remove leading hyphen
            63
        ).replaceAll("-$", "");                 // Remove trailing hyphen;
    }

    private static String portName(String applicationName, String portName) {
        return portNameLimit(
            StringUtils.join(
                StringUtils.left(applicationName,7), // Keep this small so we don't impact port uniqueness
                "-",
                rfc1123Name(portName)
            )
        );
    }

    public static String portNameLimit(String name) {
        return StringUtils.left(name,15); // Imposed by k8s
    }

    public static String dockerSecretName(String containerName) {
        return rfc1123Name(
            StringUtils.join(
                "docker-",
                containerName
            )
        );
    }

    public static String jobName(RayJobType rayJobType) {
        return switch (rayJobType) {
            case TRAIN -> "ray-train";
            case FINE_TUNE -> "ray-fine-tune";
        };
    }

    public static boolean isCustomRegistry(String url) {
        // Check if the URL contains a hostname (indicating custom registry)
        return url.contains("http://") || url.contains("https://");
    }

    public static String nodeNameFromMachineIdAndBootId(String machineId, String bootId) {
        // example machineId: 10d87568724546e5a8c33083965a97b1
        // example bootId: f9bf91fd-be9f-4215-bbc9-96f42e326635

        bootId = bootId.replace("-", "");
        bootId = bootId.substring(0, 4) +
            bootId.substring(bootId.length() / 2 - 2, bootId.length() / 2 + 2) +
            bootId.substring(bootId.length() - 4);
        machineId = machineId.substring(0, 4) +
            machineId.substring(machineId.length() / 2 - 2, machineId.length() / 2 + 2) +
            machineId.substring(machineId.length() - 4);

        return machineId+"-"+bootId;
    }

    public static Map<String, String> appLabels(String applicationName) {
        return new HashMap<>() {{
            put(SERVICE_SELECTOR_LABEL_NAME, applicationName);
            put(SERVICE_SELECTOR_LABEL_INSTANCE, applicationName);
            put(SERVICE_SELECTOR_TAG, SERVICE_SELECTOR_TAG_VALUE);
        }};
    }

    public static List<String> labelSelector(String applicationName) {
        return List.of(
            String.format("%s=%s", SERVICE_SELECTOR_LABEL_NAME, applicationName),
            String.format("%s=%s", SERVICE_SELECTOR_LABEL_INSTANCE, applicationName),
            String.format("%s=%s", SERVICE_SELECTOR_TAG, SERVICE_SELECTOR_TAG_VALUE)
        );
    }

    public static String lakeFsRepoFromHfRepo(@NotNull String hfRepo) {
        // LakeFs accepts repo name in the form: ^[a-zA-Z0-9][a-zA-Z0-9-.]{2,62}$

        // Strip 'org/' prefix if it exists (hfRepo is in the form 'org/repo')
        String repoName = hfRepo;
        if (hfRepo.contains("/")) {
            repoName = hfRepo.substring(hfRepo.lastIndexOf("/") + 1);
        }

        // Replace invalid characters with hyphens (keep alphanumeric, dots, and hyphens)
        String sanitized = repoName.replaceAll("[^a-zA-Z0-9.-]", "-")
                               .replaceAll("-+", "-")  // Replace multiple hyphens with single hyphen
                               .replaceAll("\\.+", "."); // Replace multiple dots with single dot

        // Ensure it starts with alphanumeric
        sanitized = sanitized.replaceAll("^[^a-zA-Z0-9]+", "");

        // If empty after sanitization, use default
        if (sanitized.isEmpty()) {
            return "repo-" + RandomStringUtils.secure().nextAlphanumeric(5).toLowerCase();
        }

        // Ensure minimum length of 3
        if (sanitized.length() < 3) {
            sanitized = sanitized + "-" + RandomStringUtils.secure().nextAlphanumeric(3 - sanitized.length()).toLowerCase();
        }

        // Limit to maximum length of 63
        sanitized = StringUtils.left(sanitized, 63);

        // Remove trailing non-alphanumeric characters (dots or hyphens)
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9]+$", "");

        // Final validation: ensure we still have at least 3 characters after trimming
        if (sanitized.length() < 3) {
            sanitized = sanitized + RandomStringUtils.secure().nextAlphanumeric(3 - sanitized.length()).toLowerCase();
        }

        return sanitized;
    }
}
