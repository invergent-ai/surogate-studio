package net.statemesh.service.lakefs;

import lombok.Getter;

@Getter
public enum LakeFsRepositoryType {
    MODEL("model"),
    DATASET("dataset"),;

    final String type;

    LakeFsRepositoryType(String type) {
        this.type = type;
    }
}
