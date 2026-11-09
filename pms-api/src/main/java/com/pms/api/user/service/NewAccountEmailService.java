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
                                  @Value("${app.recovery.email.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendPasswordChangeInstruction(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Your prison management system account");
        message.setText("Hello " + user.getFullName() + ",\n\n"
                + "An account has been created for you. Sign in with the credentials provided by your administrator, "
                + "then change your password immediately. If you need a password reset, use the Forgot Password option.\n\n"
                + "For your security, never share your password or send it by email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("New account email delivery failed ({})", exception.getClass().getName());
        }
    }
}
