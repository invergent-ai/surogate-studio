package net.statemesh.config;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;

public class DynamicEcrClientManager {
    private final DefaultCredentialsProvider credentialsProvider;
    private volatile EcrClient currentClient;
    private volatile Region currentRegion;

    public DynamicEcrClientManager() {
        this.credentialsProvider = DefaultCredentialsProvider.create();
    }

    public synchronized EcrClient getClient(String regionName) {
        Region newRegion = Region.of(regionName);
        if (currentClient == null || !newRegion.equals(currentRegion)) {
            if (currentClient != null) {
                currentClient.close();
            }
            currentClient = createClient(newRegion);
            currentRegion = newRegion;
        }
        return currentClient;
    }

    private EcrClient createClient(Region region) {
        return EcrClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();
    }
}
