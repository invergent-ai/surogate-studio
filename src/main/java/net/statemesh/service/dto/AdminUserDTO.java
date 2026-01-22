package net.statemesh.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.Account;
import net.statemesh.domain.Authority;
import net.statemesh.domain.enumeration.ColorScheme;
import net.statemesh.domain.enumeration.MenuMode;
import net.statemesh.domain.enumeration.UserType;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A DTO representing a user, with his authorities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    @NotBlank
    @Email
    @Size(min = 1, max = 50)
    private String login;

    String loginProvider;
    String loginProviderUserId;

    @Size(max = 100)
    private String fullName;

    @Size(max = 256)
    private String imageUrl;

    @Builder.Default
    private boolean activated = false;

    @Size(min = 2, max = 10)
    private String langKey;

    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private String lastName;
    private String firstName;
    private String company;
    private String taxCode;
    private Instant lastModifiedDate;
    private Set<String> authorities;
    private ProjectId defaultProject;
    private ZoneDTO defaultZone;
    private Instant lockedUserTime;
    private Boolean lockedOperator;
    private boolean hasApps;
    private boolean cicdPipelineAutopublish;
    private String theme;
    private ColorScheme colorScheme;
    private Integer scale;
    private MenuMode menuMode;
    private Boolean ripple;
    private String paymentMethod;
    private UserType userType;
    private Double credits;
    private Boolean dataCenter;
    private String address;
    private String city;
    private String state;
    private String country;
    private String zip;
    private String referralCode;
    private String referredByCode;

    public AdminUserDTO(Account user) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.fullName = user.getFullName();
        this.activated = user.isActivated();
        this.imageUrl = user.getImageUrl();
        this.langKey = user.getLangKey();
        this.createdBy = user.getCreatedBy();
        this.createdDate = user.getCreatedDate();
        this.lastModifiedBy = user.getLastModifiedBy();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.lastModifiedDate = user.getLastModifiedDate();
        this.authorities = user.getAuthorities().stream()
            .map(Authority::getName)
            .collect(Collectors.toSet());
//        this.defaultProject = user.getProjectIds().stream().findAny().orElse(null);
        this.defaultProject = Optional.ofNullable(user.getProjectIds())
            .orElse(Collections.emptySet())
            .stream()
            .findAny()
            .orElse(null);
        this.defaultZone = user.getDefaultZone();
        this.lockedUserTime = user.getLockedUserTime();
        this.lockedOperator = user.lockedOperator();
        this.hasApps = user.hasApps();
        this.cicdPipelineAutopublish = user.cicdPipelineAutopublish();
        this.theme = user.getTheme();
        this.ripple = user.getRipple();
        this.colorScheme = user.getColorScheme();
        this.scale = user.getScale();
        this.menuMode = user.getMenuMode();
        this.paymentMethod = user.getPaymentMethod();
        this.credits = user.getCredits();
        this.dataCenter = user.getDataCenter();
        this.userType = user.getUserType();
        this.company = user.getCompany();
        this.taxCode = user.getTaxCode();
        this.address = user.getAddress();
        this.city = user.getCity();
        this.state = user.getState();
        this.country = user.getCountry();
        this.zip = user.getZip();
        this.referralCode = user.getReferralCode();
        this.referredByCode = user.getReferredByCode();
    }
}
