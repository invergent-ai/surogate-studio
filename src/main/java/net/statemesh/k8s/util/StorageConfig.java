package net.statemesh.k8s.util;

public record StorageConfig(String smStorageBucketUrl,
                            String smStorageAccessKey,
                            String smStorageAccessSecret) {}
