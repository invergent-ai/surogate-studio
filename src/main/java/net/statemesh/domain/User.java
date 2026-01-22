package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.config.Constants;
import net.statemesh.domain.enumeration.ColorScheme;
import net.statemesh.domain.enumeration.MenuMode;
import net.statemesh.domain.enumeration.UserType;
import net.statemesh.security.AesGcmAttributeConverter;
import net.statemesh.service.dto.ProjectId;
import net.statemesh.service.dto.ZoneDTO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A user.
 */
@Entity
@Table(name = "sm_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends AbstractAuditingEntity<String> implements Account {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Email
    @Size(min = 5, max = 256)
    @Column(length = 256, unique = true, nullable = false)
    private String login;

    @Size(max = 100)
    @Column(name = "full_name", length = 100)
    private String fullName;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "password_hash", length = 60, nullable = false)
    private String password;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private boolean activated = false;

    @Size(min = 2, max = 10)
    @Column(name = "lang_key", length = 10)
    @Builder.Default
    private String langKey = Constants.DEFAULT_LANGUAGE;

    @Size(max = 256)
    @Column(name = "image_url", length = 256)
    private String imageUrl;

    @Size(max = 256)
    @Column(name = "firstname", length = 256)
    private String firstName;

    @Size(max = 256)
    @Column(name = "lastname", length = 256)
    private String lastName;

    @Size(max = 256)
    @Column(name = "company", length = 256)
    private String company;

    @Column(name = "login_provider", length = 50)
    @Size(max = 50)
    String loginProvider;

    @Column(name = "login_provider_id", length = 256)
    @Size(max = 256)
    String loginProviderId;

    @Column(name = "cli_session", length = 500)
    @Size(max = 500)
    String cliSession;

    @Column(name = "cli_token", length = 500)
    @Size(max = 500)
    String cliToken;

    @Column(name = "payment_method", length = 20)
    @Size(max = 20)
    String paymentMethod;

    @Column(name = "credits")
    Double credits;

    @Column(name = "data_center")
    Boolean dataCenter;

    @Size(max = 20)
    @Column(name = "activation_key", length = 20)
    @JsonIgnore
    private String activationKey;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    @JsonIgnore
    private String resetKey;

    @Column(name = "reset_date")
    @Builder.Default
    private Instant resetDate = null;

    @Column(name = "mobile_phone")
    private String mobilePhone;

    @Column(name = "country")
    private String country;

    @Column(name = "state")
    private String state;

    @Column(name = "city")
    private String city;

    @Column(name = "address")
    private String address;

    @Column(name = "zip")
    private String zip;

    @Column(name = "tax_code", length = 128)
    @Size(max = 128)
    private String taxCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Column(name = "agreed_terms")
    private Boolean agreedTerms;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "locked_user_time")
    private Instant lockedUserTime;

    @Column(name = "locked_operator")
    private Boolean lockedOperator;

    @Column(name = "cpu_hours_used")
    private Double cpuHoursUsed;

    @Column(name = "ram_hours_used")
    private Double ramHoursUsed;

    @Column(name = "cpu_hours_provided")
    private Double cpuHoursProvided;

    @Column(name = "ram_hours_provided")
    private Double ramHoursProvided;

    // code for others to use
    @Column(name = "referral_code")
    private String referralCode;

    // code they used
    @Column(name = "referred_by_code")
    private String referredByCode;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "sm_user_authority",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
        inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "name")}
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Authority> authorities = new HashSet<>();

    @Column(name = "notif_settings", length = 15, nullable = false)
    @ElementCollection(fetch = FetchType.EAGER, targetClass = NotifSettings.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_notif_settings", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<NotifSettings> notifSettings = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "accessLists" }, allowSetters = true)
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "cluster", "user", "units" }, allowSetters = true)
    @Builder.Default
    private Set<Node> nodes = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user", "project" }, allowSetters = true)
    @Builder.Default
    private Set<ProjectAccess> accessLists = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    @Builder.Default
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user", "organization" }, allowSetters = true)
    @Builder.Default
    private Set<UserXOrganization> organizations = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    @Builder.Default
    private Set<NodeReservation> reservations = new HashSet<>();

    @Column(name = "theme")
    private String theme;

    @Column(name = "color_scheme")
    @Enumerated(EnumType.STRING)
    private ColorScheme colorScheme;

    @Column(name = "scale")
    private Integer scale;

    @Column(name = "menu_mode")
    @Enumerated(EnumType.STRING)
    private MenuMode menuMode;

    @Column(name = "ripple")
    private Boolean ripple;

    @Column(name = "lakefs_access_key")
    private String lakeFsAccessKey;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "lakefs_secret_key", length = 512)
    private String lakeFsSecretKey;

    // Lowercase the login before saving it in database
    public void setLogin(String login) {
        this.login = StringUtils.lowerCase(login, Locale.ENGLISH);
    }

    public void setProjects(Set<Project> projects) {
        if (this.projects != null) {
            this.projects.forEach(i -> i.setUser(null));
        }
        if (projects != null) {
            projects.forEach(i -> i.setUser(this));
        }
        this.projects = projects;
    }

    public void setNodes(Set<Node> nodes) {
        if (this.nodes != null) {
            this.nodes.forEach(i -> i.setUser(null));
        }
        if (nodes != null) {
            nodes.forEach(i -> i.setUser(this));
        }
        this.nodes = nodes;
    }

    public void setAccessLists(Set<ProjectAccess> projectAccesses) {
        if (this.accessLists != null) {
            this.accessLists.forEach(i -> i.setUser(null));
        }
        if (projectAccesses != null) {
            projectAccesses.forEach(i -> i.setUser(this));
        }
        this.accessLists = projectAccesses;
    }

    public void setNotifications(Set<Notification> notifications) {
        if (this.notifications != null) {
            this.notifications.forEach(i -> i.setUser(null));
        }
        if (notifications != null) {
            notifications.forEach(i -> i.setUser(this));
        }
        this.notifications = notifications;
    }

    public void setOrganizations(Set<UserXOrganization> userXOrganizations) {
        if (this.organizations != null) {
            this.organizations.forEach(i -> i.setUser(null));
        }
        if (userXOrganizations != null) {
            userXOrganizations.forEach(i -> i.setUser(this));
        }
        this.organizations = userXOrganizations;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }

    @Override
    public Set<ProjectId> getProjectIds() {
        return null;
    }

    @Override
    public ZoneDTO getDefaultZone() {
        return null;
    }

    @Override
    public Boolean lockedOperator() {
        return lockedOperator;
    }

    @Override
    public boolean hasApps() {
        return false;
    }

    @Override
    public boolean cicdPipelineAutopublish() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        return id != null && id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    public enum NotifSettings {
        SERVICE_CONTROL,
        ACCOUNT,
        BILLING,
        NEWS,
        MAINTENANCE
    }
}
