package net.statemesh.domain.enumeration;

import lombok.Getter;
import net.statemesh.domain.User;

@Getter
public enum ProcessEvent {
    DEPLOYED("email.notification.deployed.title", NotificationType.SYSTEM, User.NotifSettings.SERVICE_CONTROL),
    PENDING("email.notification.pending.title", NotificationType.SYSTEM, User.NotifSettings.SERVICE_CONTROL),
    DELETED("email.notification.deleted.title", NotificationType.SYSTEM, User.NotifSettings.SERVICE_CONTROL),
    PAYMENT_SENT("email.notification.payment.sent.title", NotificationType.SYSTEM, User.NotifSettings.BILLING),
    PAYMENT_RECEIVED("email.notification.payment.received.title", NotificationType.SYSTEM, User.NotifSettings.BILLING),
    // Not in use
    CREATED("email.notification.created.title", NotificationType.SYSTEM, User.NotifSettings.SERVICE_CONTROL),
    UPDATED("email.notification.updated.title", NotificationType.SYSTEM, User.NotifSettings.SERVICE_CONTROL),
    ERROR("email.notification.error.title", NotificationType.SYSTEM, User.NotifSettings.SERVICE_CONTROL),
    PAYMENT_ERROR("email.notification.payment.error.title", NotificationType.SYSTEM, User.NotifSettings.BILLING);

    private final String titleKey;
    private final NotificationType type;
    private final User.NotifSettings userSetting;

    ProcessEvent(String titleKey, NotificationType type, User.NotifSettings userSetting) {
        this.titleKey = titleKey;
        this.type = type;
        this.userSetting = userSetting;
    }
}
