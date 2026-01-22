package net.statemesh.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.User;
import net.statemesh.domain.enumeration.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Service for sending emails.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
@RequiredArgsConstructor
public class MailService {
    private final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";

//    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;
    private final ApplicationProperties applicationProperties;

    @Autowired
    @Lazy
    private MailService self;

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        // Prepare message using a Spring helper
//        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//        try {
//            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
//            message.setTo(to);
//            message.setFrom(applicationProperties.getMail().getFrom());
//            message.setSubject(subject);
//            message.setText(content, isHtml);
//            javaMailSender.send(mimeMessage);
//            log.debug("Sent email to User '{}'", to);
//        } catch (MailException | MessagingException e) {
//            log.warn("Email could not be sent to user '{}'", to, e);
//        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, applicationProperties.getClientUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        self.sendEmail(user.getLogin(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        log.debug("Sending activation email to '{}'", user.getLogin());
        self.sendEmailFromTemplate(user, "mail/activationEmail.html", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getLogin());
        self.sendEmailFromTemplate(user, "mail/creationEmail.html", "email.activation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getLogin());
        self.sendEmailFromTemplate(user, "mail/passwordResetEmail.html", "email.reset.title");
    }

    @Async
    public void sendNotificationEmail(User user, ProcessEvent event, Map<String, Object> additionalVariables) {
        if (!user.getNotifSettings().contains(event.getUserSetting())) {
            log.debug("Email notifications for '{}' not enabled for user '{}'",
                event.getUserSetting(), user.getLogin());
            return;
        }

        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, applicationProperties.getClientUrl());
        context.setVariable("event", event);
        context.setVariable("notificationType", event.getType());

        additionalVariables.forEach(context::setVariable);

        String content = templateEngine.process("mail/notificationEmail.html", context);
        String subject = messageSource.getMessage(event.getTitleKey(), null, locale);
        sendEmail(user.getLogin(), subject, content, false, true);
    }
}
