package net.statemesh.domain.enumeration;


import lombok.Getter;

@Getter
public enum MenuMode {
    STATIC("static"),
    OVERLAY("overlay"),
    HORIZONTAL("horizontal"),
    SLIM("slim"),
    SLIM_PLUS("slim-plus"),
    REVEAL("reveal"),
    DRAWER("drawer");

    private final String value;

    MenuMode(String value) {
        this.value = value;
    }
}
