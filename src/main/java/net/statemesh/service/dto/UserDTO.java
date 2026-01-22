package net.statemesh.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.Account;
import net.statemesh.domain.Authority;
import net.statemesh.domain.User;
import net.statemesh.domain.enumeration.ColorScheme;
import net.statemesh.domain.enumeration.MenuMode;
import net.statemesh.domain.enumeration.UserType;

import java.io.Serial;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A DTO representing a user, with only the public attributes.
 */
@Data
@NoArgsConstructor
public class UserDTO implements Account {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String login;
    private String fullName;
    private String mobilePhone;
    private String country;
    private String state;
    private String city;
    private String address;
    private String zip;
    private Boolean agreedTerms;
    private String imageUrl;
    private boolean activated;
    private Boolean deleted;
    private Instant lockedUserTime;
    private Boolean lockedOperator;
    private String notificationSettings;
    private String activationKey;
    private String resetKey;
    private Instant resetDate;
    private String langKey;
    private String createdBy;
    private Instant createdDate = Instant.now();
    private String lastModifiedBy;
    private Instant lastModifiedDate;
    private String firstName;
    private String lastName;
    private Set<Authority> authorities;
    private Set<ProjectId> projects = new HashSet<>();
    private ZoneDTO defaultZone;
    private String theme;
    private ColorScheme colorScheme;
    private Integer scale;
    private MenuMode menuMode;
    private Boolean ripple;
    private String paymentMethod;
    private Double credits;
    private Boolean dataCenter;
    private String company;
    private String taxCode;
    private UserType userType;
    private String referralCode;
    private String referredByCode;

    // Transient
    private boolean hasApps;
    private boolean cicdPipelineAutopublish;

    public UserDTO(User user) {
        this.id = user.getId();
        // Customize it here if you need, or not, firstName/lastName/etc
        this.login = user.getLogin();
    }

    public UserDTO addProject(ProjectDTO project) {
        this.projects.add(project.idProjection());
        return this;
    }

    @Override
    public Set<ProjectId> getProjectIds() {
        return this.projects;
    }

    @Override
    public Boolean lockedOperator() {
        return lockedOperator;
    }

    @Override
    public boolean hasApps() {
        return hasApps;
    }

    @Override
    public boolean cicdPipelineAutopublish() {
        return cicdPipelineAutopublish;
    }

    public UserDTO defaultZone(ZoneDTO zone) {
        this.defaultZone = zone;
        return this;
    }

    public UserDTO hasApps(boolean hasApps) {
        this.hasApps = hasApps;
        return this;
    }

    public UserDTO cicdPipelineAutopublish(boolean cicdPipelineAutopublish) {
        this.cicdPipelineAutopublish = cicdPipelineAutopublish;
        return this;
    }
}
