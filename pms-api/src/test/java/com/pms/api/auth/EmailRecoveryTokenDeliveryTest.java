package com.pms.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import com.pms.api.auth.service.EmailRecoveryTokenDelivery;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailRecoveryTokenDeliveryTest {

    @Test
    void sendsTokenOnlyToVerifiedRecipient() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailRecoveryTokenDelivery delivery = new EmailRecoveryTokenDelivery(
                mailSender, "smtp.example.test", "security@example.test", Duration.ofMinutes(15));

        delivery.deliver(Optional.of("person@example.test"), "one-time-token");

        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertEquals("security@example.test", message.getValue().getFrom());
        assertEquals("person@example.test", message.getValue().getTo()[0]);
        assertEquals("Password reset request", message.getValue().getSubject());
        assertTrue(message.getValue().getText().contains("one-time-token"));
        assertTrue(message.getValue().getText().contains("15 minutes"));
    }

    @Test
    void ignoresAbsentRecipientWithoutSending() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailRecoveryTokenDelivery delivery = new EmailRecoveryTokenDelivery(
                mailSender, "smtp.example.test", "security@example.test", Duration.ofMinutes(15));

        delivery.deliver(Optional.empty(), "unregistered-token");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
        void keepsSmtpFailureGenericForTheCaller() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        org.mockito.Mockito.doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailRecoveryTokenDelivery delivery = new EmailRecoveryTokenDelivery(
                mailSender, "smtp.example.test", "security@example.test", Duration.ofMinutes(15));

        delivery.deliver(Optional.of("person@example.test"), "one-time-token");
    }
}