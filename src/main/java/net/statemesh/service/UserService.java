package net.statemesh.service;

import io.lakefs.clients.sdk.model.CredentialsWithSecret;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.config.Constants;
import net.statemesh.domain.Authority;
import net.statemesh.domain.User;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.domain.enumeration.UserType;
import net.statemesh.repository.AuthorityRepository;
import net.statemesh.repository.UserRepository;
import net.statemesh.security.AuthoritiesConstants;
import net.statemesh.security.SecurityUtils;
import net.statemesh.security.UserWithOAuthUser;
import net.statemesh.security.oauth2.OAuth2AuthenticationProcessingException;
import net.statemesh.security.oauth2.OAuth2UserInfo;
import net.statemesh.security.oauth2.OAuth2UserInfoFactory;
import net.statemesh.security.oauth2.SocialAuthProvider;
import net.statemesh.service.dto.*;
import net.statemesh.service.exception.InvalidPasswordException;
import net.statemesh.service.exception.UserNotFoundException;
import net.statemesh.service.exception.UsernameAlreadyUsedException;
import net.statemesh.service.k8s.ResourceControlService;
import net.statemesh.service.lakefs.LakeFsService;
import net.statemesh.service.mapper.AccountMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.UserMapper;
import net.statemesh.service.util.ProfileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static net.statemesh.config.Constants.*;

/**
 * Service class for managing users.
 */
@Service
@Transactional
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final CacheManager cacheManager;
    private final ProjectService projectService;
    private final ZoneService zoneService;
    private final ApplicationService applicationService;
    private final ResourceControlService resourceControlService;
    private final AsyncTaskExecutor smTaskExecutor;
    private final ApplicationProperties applicationProperties;
    private final Environment environment;
    private ZoneDTO defaultZone;
    private final LakeFsService lakeFsService;

    @PostConstruct
    public void init() {
        if (ProfileUtil.isAppliance(environment)) {
            this.defaultZone =
                zoneService.findByZoneId(DEFAULT_DENSEMAX_ZONE_ID.getValue()).orElse(null);
        } else {
            this.defaultZone =
                zoneService.findByZoneId(DEFAULT_CLOUD_ZONE_ID.getValue()).orElse(null);
        }
    }

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository
            .findOneByActivationKey(key)
            .map(this::activateUser);
    }

    private User activateUser(User user) {
        // activate given user for the registration key.
        user.setActivated(true);
        user.setActivationKey(null);
        user = userRepository.save(user);

        this.clearUserCaches(user);
        log.debug("Activated user: {}", user);
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> searchByName(String query) {
        log.debug("Request to search Users by name containing : {}", query);

        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll()
                .stream()
                .map(userMapper::userToUserDTO)
                .collect(Collectors.toList());
        }

        return userRepository.findByFullNameContainingIgnoreCase(query.trim())
            .stream()
            .map(userMapper::userToUserDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public void softDeleteUser(String login) {
        userRepository
            .findOneByLoginIgnoreCase(login)
            .ifPresent(user -> {
                user.setDeleted(true);
                user.setLockedUserTime(Instant.now());
                userRepository.save(user);
                this.clearUserCaches(user);
                stopUserAssets(login);
                log.debug("Soft deleted User: {}", user);
            });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void stopUserAssets(String login) {
        smTaskExecutor.submit(() -> {
            this.resourceControlService.stopUserApplications(login);
        });
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAllBasicInfo() {
        log.debug("Request to get all Users basic info");
        return userRepository.findAll()
            .stream()
            .map(user -> {
                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setFullName(user.getFullName());
                return dto;
            })
            .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public NotificationSettingsDTO getUserNotificationSettings(String login) {
        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));

        Map<String, Boolean> settings = new HashMap<>();

        // Initialize all possible settings with default values
        Arrays.stream(User.NotifSettings.values()).forEach(type -> {
            boolean enabled = user.getNotifSettings().contains(type);
            settings.put(type.name(), enabled);
        });

        return new NotificationSettingsDTO(settings);
    }

    @Transactional
    public void updateUserNotificationSettings(String login, NotificationSettingsDTO settingsDTO) {
        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));

        Set<User.NotifSettings> updatedSettings = new HashSet<>();

        settingsDTO.getSettings().forEach((typeStr, enabled) -> {
            try {
                User.NotifSettings type = User.NotifSettings.valueOf(typeStr);
                if (enabled) {
                    updatedSettings.add(type);
                }
            } catch (IllegalArgumentException e) {
                // Handle invalid notification type
                log.warn("Invalid notification type received: {}", typeStr);
            }
        });

        user.setNotifSettings(updatedSettings);
        userRepository.save(user);
        clearUserCaches(user);
    }


    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository
            .findOneByResetKey(key)
            .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                this.clearUserCaches(user);
                return user;
            });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository
            .findOneByLoginIgnoreCase(mail)
            .filter(User::isActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                this.clearUserCaches(user);
                return user;
            });
    }

    public User registerUser(AdminUserDTO userDTO, String password) {
        userRepository
            .findOneByLoginIgnoreCase(userDTO.getLogin().toLowerCase())
            .ifPresent(existingUser -> {
                boolean removed = removeNonActivatedUser(existingUser);
                if (!removed) {
                    throw new UsernameAlreadyUsedException();
                }
            });

        User newUser = new User();

        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin().toLowerCase());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFullName(userDTO.getFullName());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        newUser.setLoginProvider(userDTO.getLoginProvider());
        newUser.setLoginProviderId(userDTO.getLoginProviderUserId());
        newUser.setPaymentMethod(PAYMENT_METHOD_CARD);
        newUser.setUserType(UserType.INDIVIDUAL);
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);

        // If the user was referred by another user, set the referred by code
        Optional<User> referrer = Optional.empty();
        if (userDTO.getReferralCode() != null) {
            referrer = userRepository.findByReferralCode(userDTO.getReferralCode());
            if (referrer.isPresent()) {
                newUser.setReferredByCode(userDTO.getReferralCode());
            }
        }

        // Generate a new referral code for the user
        newUser.setReferralCode(UUID.randomUUID().toString().substring(0, 8));
        newUser = userRepository.save(newUser);

        this.clearUserCaches(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public UserWithOAuthUser processOauthRegistration(
        String registrationId,
        Map<String, Object> attributes,
        OidcIdToken idToken,
        OidcUserInfo userInfo,
        Map<String, Object> additionalParameters
    ) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);
        if (StringUtils.isEmpty(oAuth2UserInfo.getName())) {
            throw new OAuth2AuthenticationProcessingException("Name not found from OAuth2 provider");
        } else if (StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Name not found from OAuth2 provider");
        }

        User user = userRepository.findOneWithAuthoritiesByLoginIgnoreCase(oAuth2UserInfo.getEmail()).orElse(null);
        if (user == null) {
            String refferalCode = (String) additionalParameters.get("referralCode");
            user = registerUser(
                AdminUserDTO.builder()
                    .login(oAuth2UserInfo.getEmail())
                    .fullName(oAuth2UserInfo.getName())
                    .imageUrl(oAuth2UserInfo.getImageUrl())
                    .loginProvider(toSocialProvider(registrationId).getProviderType())
                    .loginProviderUserId(oAuth2UserInfo.getId())
                    .langKey(Constants.DEFAULT_LANGUAGE)
                    .referralCode(refferalCode)
                    .activated(false)
                    .build(),
                RandomUtil.generatePassword()
            );
            activateUser(user);
        }

        return UserWithOAuthUser.create(user, attributes, idToken, userInfo);
    }

    private SocialAuthProvider toSocialProvider(String providerId) {
        for (SocialAuthProvider socialProvider : SocialAuthProvider.values()) {
            if (socialProvider.getProviderType().equals(providerId)) {
                return socialProvider;
            }
        }
        return SocialAuthProvider.LOCAL;
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        this.clearUserCaches(existingUser);
        return true;
    }

    public User createUser(AdminUserDTO userDTO) {
        User user = new User();
        user.setLogin(userDTO.getLogin().toLowerCase());
        user.setMobilePhone(userDTO.getFullName());
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        String generatedPass = RandomUtil.generatePassword();
        log.debug("Generated Password: {}", generatedPass);
        String encryptedPassword = passwordEncoder.encode(generatedPass);
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO
                .getAuthorities()
                .stream()
                .map(authorityRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        CredentialsWithSecret credentialsWithSecret = lakeFsService.createUser(userDTO.getLogin());
        log.debug("createUser() - aK: {}, sk: {}", credentialsWithSecret.getAccessKeyId(), credentialsWithSecret.getSecretAccessKey());
        user.setLakeFsAccessKey(credentialsWithSecret.getAccessKeyId());
        user.setLakeFsSecretKey(credentialsWithSecret.getSecretAccessKey());


        userRepository.save(user);
        this.clearUserCaches(user);
        return user;
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    public Optional<AdminUserDTO> updateUser(AdminUserDTO userDTO) {
        return Optional
            .of(userRepository.findById(userDTO.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(user -> {
                this.clearUserCaches(user);
                user.setLogin(userDTO.getLogin().toLowerCase());
                user.setFullName(userDTO.getFullName());
                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setLangKey(userDTO.getLangKey());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                userDTO
                    .getAuthorities()
                    .stream()
                    .map(authorityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(managedAuthorities::add);
                userRepository.save(user);
                this.clearUserCaches(user);
                log.debug("Changed Information for User: {}", user);

                return user;
            })
            .map(AdminUserDTO::new);
    }

    public void deleteUser(String login) {
        userRepository
            .findOneByLoginIgnoreCase(login)
            .ifPresent(user -> {
                userRepository.delete(user);
                this.clearUserCaches(user);
                log.debug("Deleted User: {}", user);
            });
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param fullName name of user.
     * @param langKey  language key.
     * @param imageUrl image URL of user.
     * @param userType - userType
     * @param company  - company
     * @param taxCode  - taxCode
     * @param address  - address
     * @param city     - city
     * @param state    - state
     * @param country  - country
     * @param zip      - zip
     */
    public void updateUser(
        String fullName,
        String langKey,
        String imageUrl,
        String firstname,
        String lastname,
        UserType userType,
        String company,
        String taxCode,
        String address,
        String city,
        String state,
        String country,
        String zip
    ) {
        SecurityUtils
            .getCurrentUserLogin()
            .flatMap(userRepository::findOneByLoginIgnoreCase)
            .ifPresent(user -> {
                user.setFullName(fullName);
                user.setLangKey(langKey);
                user.setImageUrl(imageUrl);
                user.setFirstName(firstname);
                user.setLastName(lastname);
                user.setUserType(userType);
                user.setCompany(company);
                user.setTaxCode(taxCode);
                user.setAddress(address);
                user.setCity(city);
                user.setState(state);
                user.setCountry(country);
                user.setZip(zip);
                userRepository.save(user);
                this.clearUserCaches(user);
                log.debug("Updated User: {}", user);
            });
    }

    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils
            .getCurrentUserLogin()
            .flatMap(userRepository::findOneByLoginIgnoreCase)
            .ifPresent(user -> {
                String currentEncryptedPassword = user.getPassword();
                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw new InvalidPasswordException();
                }
                String encryptedPassword = passwordEncoder.encode(newPassword);
                user.setPassword(encryptedPassword);
                this.clearUserCaches(user);
                log.debug("Changed password for User: {}", user);
            });
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllPublicUsers(Pageable pageable) {
        return userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLoginIgnoreCase(login);
    }

    @Transactional
    public Optional<UserDTO> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneWithAuthoritiesByLoginIgnoreCase)
            .map(u -> accountMapper.toDto(u, new CycleAvoidingMappingContext()))
            .map(this::ensureDefaultProject)
            .map(this::ensureDatacentersOnProjects)
            .map(this::ensureRayClustersOnProjects)
            .map(this::ensureDefaultZone)
            .map(this::ensureUserHasApps)
            .map(this::ensureAppProperties)
            .map(this::ensureNotDeleted);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired every day, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
            .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS))
            .forEach(user -> {
                log.debug("Deleting not activated user {}", user.getLogin());
                userRepository.delete(user);
                this.clearUserCaches(user);
            });
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> findOne(String login) {
        log.debug("Request to get User : {}", login);
        return userRepository.findOneByLoginIgnoreCase(login)
            .map(userMapper::userToUserDTO);
    }

    @Transactional(readOnly = true)
    public Optional<User> findOneSimple(String login) {
        log.debug("Request to get User : {}", login);
        return userRepository.findOneByLoginIgnoreCase(login);
    }

    /**
     * Gets a list of all the authorities.
     *
     * @return a list of all the authorities.
     */
    @Transactional(readOnly = true)
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).toList();
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
    }

    private UserDTO ensureDefaultProject(UserDTO user) {
        return user.addProject(
            projectService.getByUser(user.getLogin()).stream()
                .filter(project -> !Optional.ofNullable(project.getDeleted()).orElse(Boolean.FALSE))
                .findAny()
                .orElseGet(() ->
                    projectService.save(
                        ProjectDTO.builder()
                            .name(DEFAULT_PROJECT_NAME)
                            .description(DEFAULT_PROJECT_DESCRIPTION)
                            .profile(ProfileUtil.isAppliance(environment) ? Profile.GPU : Profile.HYBRID)
                            .zone(defaultZone)
                            .user(user)
                            .build(),
                        user.getLogin()
                    )
                )
        );
    }

    private UserDTO ensureDatacentersOnProjects(UserDTO user) {
        if (applicationProperties.getProfile() != null && applicationProperties.getProfile().getDatacenters() != null &&
            !applicationProperties.getProfile().getDatacenters().isEmpty()) {
            projectService.getByUser(user.getLogin()).stream()
                .filter(project -> !Optional.ofNullable(project.getDeleted()).orElse(Boolean.FALSE))
                .filter(project -> StringUtils.isEmpty(project.getDatacenterName()))
                .forEach(project ->
                    projectService.save(
                        project.datacenterName(
                            // We could implement a datacenter selection strategy
                            applicationProperties.getProfile().getDatacenters().parallelStream().findAny().orElse(null)
                        ),
                        user.getLogin()
                    )
                );
        }

        return user;
    }

    private UserDTO ensureRayClustersOnProjects(UserDTO user) {
        if (applicationProperties.getProfile() != null && applicationProperties.getProfile().getRayClusters() != null &&
            !applicationProperties.getProfile().getRayClusters().isEmpty()) {
            projectService.getByUser(user.getLogin()).stream()
                .filter(project -> !Optional.ofNullable(project.getDeleted()).orElse(Boolean.FALSE))
                .filter(project -> StringUtils.isEmpty(project.getRayCluster()))
                .forEach(project ->
                    projectService.save(
                        project.rayCluster(
                            // We could implement a ray cluster selection strategy
                            applicationProperties.getProfile().getRayClusters().parallelStream()
                                .map(ApplicationProperties.RayCluster::getName)
                                .findAny()
                                .orElse(null)
                        ),
                        user.getLogin()
                    )
                );
        }

        return user;
    }

    @Transactional(readOnly = true)
    public ThemeSettingsDTO getUserThemeSettings(String login) {
        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));

        ThemeSettingsDTO settings = new ThemeSettingsDTO();
        settings.setTheme(user.getTheme());
        settings.setColorScheme(user.getColorScheme());
        settings.setScale(user.getScale());
        settings.setMenuMode(user.getMenuMode());
        settings.setRipple(user.getRipple());

        return settings;
    }

    @Transactional
    public void updateUserThemeSettings(String login, ThemeSettingsDTO settings) {
        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));

        user.setTheme(settings.getTheme());
        user.setColorScheme(settings.getColorScheme());
        user.setScale(settings.getScale());
        user.setMenuMode(settings.getMenuMode());
        user.setRipple(settings.getRipple());

        userRepository.save(user);
        clearUserCaches(user);
    }

    @Transactional
    public void updatePaymentMethod(String login, String method) {
        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));
        user.setPaymentMethod(method);
        userRepository.save(user);
        clearUserCaches(user);
    }

    @Transactional
    public void updateCreditBalance(String login, Double amount) {
        User user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new UserNotFoundException("User could not be found"));
        Double credits = Optional.ofNullable(user.getCredits()).orElse(0d);
        user.setCredits(credits + amount);
        user = userRepository.save(user);
        clearUserCaches(user);
    }

    @Transactional
    public void lockUserForNonPayingApps(String user, Instant time) {
        userRepository.lockUserForNonPayingApps(user, time);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByCliSession(String cliSession) {
        return userRepository.findByCliSession(cliSession);
    }

    @Transactional
    public void setCliSession(String login, String session, String token) {
        userRepository.setCliSession(login, session);
        userRepository.setCliToken(login, token);
    }

    @Transactional
    public void resetCliSession(String login) {
        userRepository.resetCliSession(login);
    }

    private UserDTO ensureDefaultZone(UserDTO user) {
        return user.defaultZone(defaultZone);
    }

    private UserDTO ensureUserHasApps(UserDTO user) {
        return user.hasApps(
            applicationService.userHasApps(user)
        );
    }

    private UserDTO ensureAppProperties(UserDTO user) {
        return user.cicdPipelineAutopublish(applicationProperties.getPipeline().isCicdPipelineAutopublish());
    }

    private UserDTO ensureNotDeleted(UserDTO user) {
        if (user.getDeleted() != null && user.getDeleted()) {
            throw new RuntimeException("User " + user.getLogin() + " was deleted");
        }
        return user;
    }
}
