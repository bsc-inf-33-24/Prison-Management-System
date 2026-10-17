package com.pms.api.auth.service;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.recovery.email.enabled", havingValue = "true")
public class EmailRecoveryTokenDelivery implements RecoveryTokenDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRecoveryTokenDelivery.class);
    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String fromAddress;
    private final Duration tokenTtl;

    public EmailRecoveryTokenDelivery(JavaMailSender mailSender,
                                      @Value("${spring.mail.host:}") String mailHost,
                                      @Value("${app.recovery.email.from:}") String fromAddress,
                                      @Value("${auth.password-reset-ttl:PT15M}") Duration tokenTtl) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
        this.tokenTtl = tokenTtl;
        if (mailHost.isBlank() || fromAddress.isBlank()) {
            throw new IllegalStateException("Enabled recovery email requires SMTP host and sender configuration.");
        }
    }

    @Override
    public void deliver(Optional<String> recipient, String rawToken) {
        if (recipient.isEmpty()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient.get());
        message.setSubject("Password reset request");
        message.setText("Use this one-time password reset token to reset your account password:\n\n"
                + rawToken + "\n\nThe token expires in " + tokenTtl.toMinutes()
                + " minutes. If you did not request this, you can ignore this message.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Password recovery email delivery failed ({})", exception.getClass().getName());
        }
    }
}