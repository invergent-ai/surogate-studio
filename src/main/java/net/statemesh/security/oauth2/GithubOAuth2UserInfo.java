package net.statemesh.security.oauth2;

import java.util.Map;

public class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return ((Integer) attributes.get("id")).toString();
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        if (attributes.get("email") == null) {
            //  ID+USERNAME@users.noreply.github.com
            return String.format("%s+%s@users.noreply.github.com", getId(), attributes.get("login"));
        } else {
            return (String) attributes.get("email");
        }
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}
