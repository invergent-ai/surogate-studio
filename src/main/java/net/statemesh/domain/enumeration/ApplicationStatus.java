package net.statemesh.domain.enumeration;

/**
 * Application status is their normal order;
 * CREATED -> BUILDING (optional) -> INITIALIZED -> DEPLOYING -> DEPLOYED
 */
public enum ApplicationStatus {
    CREATED,
    BUILDING,
    INITIALIZED,
    DEPLOYING,
    DEPLOYED,
    DELETING,
    ERROR
}
