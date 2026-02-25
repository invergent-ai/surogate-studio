package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Account", description = "User account management, registration, and authentication")
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

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid password or login already used")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        mailService.sendActivationEmail(user);
    }

    @Operation(summary = "Activate a registered user")
    @ApiResponse(responseCode = "200", description = "User activated successfully")
    @ApiResponse(responseCode = "500", description = "No user found for activation key")
    @GetMapping("/activate")
    public void activateAccount(
        @Parameter(description = "Activation key") @RequestParam(value = "key", name = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (user.isEmpty()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }

    @Operation(summary = "Delete current user account")
    @ApiResponse(responseCode = "200", description = "Account deleted successfully")
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

    @Operation(summary = "Get current user account")
    @ApiResponse(responseCode = "200", description = "Account returned successfully")
    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        return userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new)
            .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    @Operation(summary = "Update current user account")
    @ApiResponse(responseCode = "200", description = "Account updated successfully")
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

    @Operation(summary = "Get notification settings")
    @ApiResponse(responseCode = "200", description = "Notification settings returned")
    @GetMapping("/account/notification-settings")
    public ResponseEntity<NotificationSettingsDTO> getNotificationSettings() {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        NotificationSettingsDTO settings = userService.getUserNotificationSettings(userLogin);
        return ResponseEntity.ok(settings);
    }

    @Operation(summary = "Update notification settings")
    @ApiResponse(responseCode = "200", description = "Notification settings updated")
    @PutMapping("/account/notification-settings")
    public ResponseEntity<Void> updateNotificationSettings(@Valid @RequestBody NotificationSettingsDTO settingsDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        userService.updateUserNotificationSettings(userLogin, settingsDTO);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change password")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid password")
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    @Operation(summary = "Request password reset", description = "Send email to reset password")
    @ApiResponse(responseCode = "200", description = "Reset email sent if account exists")
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        Optional<User> user = userService.requestPasswordReset(mail);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.orElseThrow());
        } else {
            log.warn("Password reset requested for non existing mail");
        }
    }

    @Operation(summary = "Finish password reset", description = "Complete password reset with key and new password")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid password")
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

    @Operation(summary = "Search users by name")
    @ApiResponse(responseCode = "200", description = "Users returned")
    @GetMapping("users/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
        @Parameter(description = "Search query") @RequestParam("query") String query) {
        log.debug("REST request to search Users by name containing : {}", query);
        return ResponseEntity.ok(userService.searchByName(query));
    }

    @Operation(summary = "Get all users basic info")
    @ApiResponse(responseCode = "200", description = "Basic info returned")
    @GetMapping("users/basic")
    public ResponseEntity<List<UserDTO>> getBasicInfo() {
        log.debug("REST request to get all Users basic info");
        return ResponseEntity.ok(userService.findAllBasicInfo());
    }

    @Operation(summary = "Get theme settings")
    @ApiResponse(responseCode = "200", description = "Theme settings returned")
    @GetMapping("/account/theme-settings")
    public ResponseEntity<ThemeSettingsDTO> getThemeSettings() {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        ThemeSettingsDTO settings = userService.getUserThemeSettings(userLogin);
        return ResponseEntity.ok(settings);
    }

    @Operation(summary = "Update theme settings")
    @ApiResponse(responseCode = "200", description = "Theme settings updated")
    @PutMapping("/account/theme-settings")
    public ResponseEntity<Void> updateThemeSettings(@Valid @RequestBody ThemeSettingsDTO settingsDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        userService.updateUserThemeSettings(userLogin, settingsDTO);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update payment method")
    @ApiResponse(responseCode = "200", description = "Payment method updated")
    @PutMapping("/account/payment-method")
    public ResponseEntity<Void> updatePaymentMethod(
        @Parameter(description = "Payment method (card or crypto)") @RequestParam("method") String paymentMethod) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        if (StringUtils.isEmpty(paymentMethod)) {
            throw new AccountResourceException("Payment method cannot be empty");
        }
        if (!List.of("card", "crypto").contains(paymentMethod)) {
            throw new AccountResourceException("Invalid payment method");
        }
        userService.updatePaymentMethod(userLogin, paymentMethod);
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
