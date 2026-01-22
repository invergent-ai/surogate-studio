package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.Notification;
import net.statemesh.domain.User;
import net.statemesh.domain.enumeration.NotificationType;
import net.statemesh.domain.enumeration.ProcessEvent;
import net.statemesh.repository.NotificationRepository;
import net.statemesh.repository.UserRepository;
import net.statemesh.service.dto.NotificationDTO;
import net.statemesh.service.dto.ResourceDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Notification}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    public static final String PARAM_AMOUNT = "amount";
    public static final String PARAM_CURRENCY = "currency";

    private final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final MailService mailService;
    private final MessageSource messageSource;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;


    @MessageMapping("/topic/notification")
    @SendTo("/topic/notifications")
    public NotificationDTO sendNotification(@Payload NotificationDTO notification) {
        log.debug("Sending notification data {}", notification);
        return notification;
    }

    // Method to be called from NotificationService to broadcast notifications
    public void broadcastNotification(NotificationDTO notification, String userLogin) {
        log.debug("Broadcasting notification to user {}: {}", userLogin, notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + userLogin, notification);
    }

    /**
     * Save a notification.
     *
     * @param notificationDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificationDTO save(NotificationDTO notificationDTO) {
        log.debug("Request to save Notification : {}", notificationDTO);
        Notification notification = notificationMapper.toEntity(notificationDTO, new CycleAvoidingMappingContext());
        notification = notificationRepository.save(notification);
        return notificationMapper.toDto(notification, new CycleAvoidingMappingContext());
    }

    /**
     * Update a notification.
     *
     * @param notificationDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificationDTO update(NotificationDTO notificationDTO) {
        log.debug("Request to update Notification : {}", notificationDTO);
        Notification notification = notificationMapper.toEntity(notificationDTO, new CycleAvoidingMappingContext());
        notification = notificationRepository.save(notification);
        return notificationMapper.toDto(notification, new CycleAvoidingMappingContext());
    }

    /**
     * Get all the notifications.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Notifications");
        return notificationRepository.findAll(pageable).map(o -> notificationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the notifications with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<NotificationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return notificationRepository.findAllWithEagerRelationships(pageable)
            .map(o -> notificationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }


    public Page<NotificationDTO> findByUserLogin(String login, Pageable pageable) {
        return notificationRepository.findByUserLoginAndReadIsFalseOrderByCreatedTimeDesc(login, pageable)
            .map(notification -> notificationMapper.toDto(notification, new CycleAvoidingMappingContext()));
    }


    @Transactional
    public void markAllAsRead(String login) {
        notificationRepository.markAllAsRead(login);
    }

    @Transactional
    public void markAsRead(String id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Get one notification by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationDTO> findOne(String id) {
        log.debug("Request to get Notification : {}", id);
        return notificationRepository.findOneWithEagerRelationships(id)
            .map(o -> notificationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the notification by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Notification : {}", id);
        notificationRepository.deleteById(id);
    }

    @Transactional
    public void notifyUser(
        ResourceDTO resource,
        String login,
        ProcessEvent event,
        Map<String, String> params,
        boolean sendEmail
    ) {
        log.trace("Notifying user {} of resource {} event {} with params {}", login, resource.getName(), event, params);
        final User user = userRepository.findOneByLoginIgnoreCase(login).orElse(null);
        if (user == null) {
            return;
        }

        try {
            createDatabaseNotification(resource, user, event, params);
        } catch (Exception e) {
            log.error("Failed to create database notification");
        }

        if (sendEmail) {
            try {
                sendEmailNotification(resource, user, event, params);
            } catch (Exception e) {
                log.error("Failed to send email notification");
            }
        }
    }

    @Transactional
    public void createDatabaseNotification(
        ResourceDTO resourceDTO,
        User user,
        ProcessEvent event,
        Map<String, String> params
    ) {
        Map<String, String> extraProperties = new HashMap<>();
        extraProperties.put("resourceId", resourceDTO.getId());
        extraProperties.put("resourceName", resourceDTO.getName());
        extraProperties.put("event", event.toString());
        if (params != null) {
            extraProperties.putAll(params);
        }

        var notification = notificationRepository.save(
            Notification.builder()
                .message(createNotificationMessage(resourceDTO, event, params))
                .user(user)
                .type(NotificationType.SYSTEM)
                .createdTime(LocalDateTime.now())
                .extraProperties(extraProperties)
                .build()
        );

        broadcastNotification(
            notificationMapper.toDto(notification, new CycleAvoidingMappingContext()),
            user.getLogin()
        );
    }

    private String createNotificationMessage(ResourceDTO resourceDTO, ProcessEvent event, Map<String, String> params) {
        switch (event) {
            case PAYMENT_SENT -> {
                String amountWithCurrency = String.format("%.6f %s", Double.parseDouble(params.get(PARAM_AMOUNT)), params.get(PARAM_CURRENCY));
                return messageSource.getMessage(
                    "email.notification.payment.sent.message",
                    new Object[]{amountWithCurrency},
                    LocaleContextHolder.getLocale()
                );
            }
            case PAYMENT_RECEIVED -> {
                String amountWithCurrency = String.format("%.6f %s", Double.parseDouble(params.get(PARAM_AMOUNT)), params.get(PARAM_CURRENCY));
                return messageSource.getMessage(
                    "email.notification.payment.received.message",
                    new Object[]{amountWithCurrency},
                    LocaleContextHolder.getLocale()
                );
            }
            default -> {
                return messageSource.getMessage(
                    "email.notification." + event.name().toLowerCase() + ".message",
                    new Object[]{resourceDTO.getName()},
                    LocaleContextHolder.getLocale()
                );
            }
        }
    }

    private void sendEmailNotification(ResourceDTO resourceDTO, User user, ProcessEvent event, Map<String, String> params) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("resourceName", resourceDTO.getName());
        variables.put("timestamp", LocalDateTime.now());

        // Add all the notification params to email variables
        if (params != null) {
            variables.putAll(params);
        }

        mailService.sendNotificationEmail(user, event, variables);
    }
}
