package net.statemesh.security;

import lombok.Getter;
import lombok.Setter;
import net.statemesh.domain.Authority;
import net.statemesh.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

public class UserWithOAuthUser extends org.springframework.security.core.userdetails.User implements OAuth2User, OidcUser {
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;
    @Setter
    private Map<String, Object> attributes;
    @Getter
    private final User user;

    public UserWithOAuthUser(final String userID, final String password, final boolean enabled, final boolean accountNonExpired, final boolean credentialsNonExpired,
                             final boolean accountNonLocked, final Collection<? extends GrantedAuthority> authorities, final User user) {
        this(userID, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities, user, null, null);
    }

    public UserWithOAuthUser(final String userID, final String password, final boolean enabled, final boolean accountNonExpired, final boolean credentialsNonExpired,
                             final boolean accountNonLocked, final Collection<? extends GrantedAuthority> authorities, final User user, OidcIdToken idToken,
                             OidcUserInfo userInfo) {
        super(userID, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.user = user;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public static UserWithOAuthUser create(User user, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        UserWithOAuthUser userWithOAuthUser = new UserWithOAuthUser(
            user.getLogin(),
            user.getPassword(),
            true,
            true,
            true,
            true,
            buildSimpleGrantedAuthorities(user.getAuthorities()),
            user,
            idToken,
            userInfo
        );
        userWithOAuthUser.setAttributes(attributes);
        return userWithOAuthUser;
    }

    @Override
    public String getName() {
        return this.user.getLogin();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Map<String, Object> getClaims() {
        return this.attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return this.userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return this.idToken;
    }

    public static List<SimpleGrantedAuthority> buildSimpleGrantedAuthorities(Set<Authority> roles) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Authority role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        }
        return authorities;
    }
}
