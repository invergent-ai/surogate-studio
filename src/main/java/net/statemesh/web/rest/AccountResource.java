package net.statemesh.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.statemesh.domain.User;
import net.statemesh.repository.UserRepository;
import net.statemesh.security.SecurityUtils;
import net.statemesh.service.MailService;
import net.statemesh.service.UserService;
import net.statemesh.service.dto.*;
import net.statemesh.service.exception.InvalidPasswordException;
import net.statemesh.service.exception.LoginAlreadyUsedException;
import net.statemesh.web.rest.vm.KeyAndPasswordVM;
import net.statemesh.web.rest.vm.ManagedUserVM;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountResource {
    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    private static class AccountResourceException extends RuntimeException {
        private AccountResourceException(String message) {
            super(message);
        }
    }

    private final UserRepository userRepository;
    private final UserService userService;
    private final MailService mailService;

    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if the password is incorrect.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        mailService.sendActivationEmail(user);
    }

    /**
     * {@code GET  /activate} : activate the registered user.
     *
     * @param key the activation key.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be activated.
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key", name = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (user.isEmpty()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }


    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount() {
        String userLogin = SecurityUtils
            .getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));

        Optional<User> user = userRepository.findOneByLoginIgnoreCase(userLogin);
        if (user.isEmpty()) {
            throw new AccountResourceException("User could not be found");
        }

        userService.softDeleteUser(userLogin);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        return userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new)
            .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        String userLogin = SecurityUtils
            .getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> user = userRepository.findOneByLoginIgnoreCase(userLogin);
        if (user.isEmpty()) {
            throw new AccountResourceException("User could not be found");
        }
        userService.updateUser(
            userDTO.getFullName(),
            userDTO.getLangKey(),
            userDTO.getImageUrl(),
            userDTO.getFirstName(),
            userDTO.getLastName(),
            userDTO.getUserType(),
            userDTO.getCompany(),
            userDTO.getTaxCode(),
            userDTO.getAddress(),
            userDTO.getCity(),
            userDTO.getState(),
            userDTO.getCountry(),
            userDTO.getZip()
        );
    }

    @GetMapping("/account/notification-settings")
    public ResponseEntity<NotificationSettingsDTO> getNotificationSettings() {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));

        NotificationSettingsDTO settings = userService.getUserNotificationSettings(userLogin);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/account/notification-settings")
    public ResponseEntity<Void> updateNotificationSettings(@Valid @RequestBody NotificationSettingsDTO settingsDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));

        userService.updateUserNotificationSettings(userLogin, settingsDTO);
        return ResponseEntity.ok().build();
    }


    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST   /account/reset-password/init} : Send email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        Optional<User> user = userService.requestPasswordReset(mail);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.orElseThrow());
        } else {
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
            log.warn("Password reset requested for non existing mail");
        }
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (user.isEmpty()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    @GetMapping("users/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam("query") String query) {
        log.debug("REST request to search Users by name containing : {}", query);
        return ResponseEntity.ok(userService.searchByName(query));
    }

    @GetMapping("users/basic")
    public ResponseEntity<List<UserDTO>> getBasicInfo() {
        log.debug("REST request to get all Users basic info");
        return ResponseEntity.ok(userService.findAllBasicInfo());
    }

    @GetMapping("/account/theme-settings")
    public ResponseEntity<ThemeSettingsDTO> getThemeSettings() {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));

        ThemeSettingsDTO settings = userService.getUserThemeSettings(userLogin);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/account/theme-settings")
    public ResponseEntity<Void> updateThemeSettings(@Valid @RequestBody ThemeSettingsDTO settingsDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));

        userService.updateUserThemeSettings(userLogin, settingsDTO);
        return ResponseEntity.ok().build();
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (
            StringUtils.isEmpty(password) ||
                password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
                password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH
        );
    }
}
