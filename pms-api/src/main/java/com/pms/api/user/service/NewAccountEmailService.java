package com.pms.api.user.service;

import com.pms.api.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.recovery.email.enabled", havingValue = "true")
public class NewAccountEmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewAccountEmailService.class);
    private final JavaMailSender mailSender;
    private final String from;

    public NewAccountEmailService(JavaMailSender mailSender,
                                  @Value("${spring.mail.host:}") String host,
                                  @Value("${app.recovery.email.from:}") String from) {
        if (host.isBlank() || from.isBlank())
            throw new IllegalStateException("Enabled account email requires SMTP host and sender configuration.");
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendTemporaryPassword(User user, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Your prison management system account password");
        message.setText("Hello " + user.getFullName() + ",\n\n"
                + "An account has been created for you. Sign in with username " + user.getUsername()
                + " and this temporary password:\n\n" + temporaryPassword + "\n\n"
                + "For security, change this password after signing in using the Change Password option.\n\n"
                + "For your security, do not share this email or password.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("New account email delivery failed ({})", exception.getClass().getName());
            throw new AccountEmailDeliveryException();
        }
    }

    public static class AccountEmailDeliveryException extends RuntimeException { }
}
