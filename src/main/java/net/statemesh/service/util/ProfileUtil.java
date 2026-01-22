package net.statemesh.service.util;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.env.Environment;

import static net.statemesh.config.Constants.PROFILE_CLOUD;
import static net.statemesh.config.Constants.PROFILE_APPLIANCE;

public class ProfileUtil {
    public static boolean isCloud(Environment environment) {
        return ArrayUtils.contains(environment.getActiveProfiles(), PROFILE_CLOUD);
    }

    public static boolean isAppliance(Environment environment) {
        return ArrayUtils.contains(environment.getActiveProfiles(), PROFILE_APPLIANCE);
    }

    public static boolean isDevelopment(Environment environment) {
        return ArrayUtils.contains(environment.getActiveProfiles(), "dev");
    }
}
