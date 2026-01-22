package net.statemesh.domain.enumeration;

import lombok.Getter;

/**
 * Pull image mode ENUM
 */
@Getter
public enum PullImageMode {
    LOCAL_FIRST("IfNotPresent"),
    PULL("Always"),
    LOCAL_ONLY("Never");

    private String value;

    PullImageMode(String value) {
        this.value = value;
    }
}
