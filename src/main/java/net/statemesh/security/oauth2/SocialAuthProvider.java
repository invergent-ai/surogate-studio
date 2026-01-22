package net.statemesh.security.oauth2;

import lombok.Getter;

@Getter
public enum SocialAuthProvider {
    LOCAL("local"),
    GOOGLE("google"),
    GITHUB("github");

    private final String providerType;

    SocialAuthProvider(final String providerType) {
        this.providerType = providerType;
    }
}
