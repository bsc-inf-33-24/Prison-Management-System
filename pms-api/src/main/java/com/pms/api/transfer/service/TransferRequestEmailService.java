package com.pms.api.transfer.service;

import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
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
public class TransferRequestEmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferRequestEmailService.class);

    private final JavaMailSender mailSender;
    private final UserRepository users;
    private final String from;

    public TransferRequestEmailService(JavaMailSender mailSender, UserRepository users,
                                       @Value("${app.recovery.email.from:}") String from) {
        this.mailSender = mailSender;
        this.users = users;
        this.from = from;
    }

    public void notifyCommissioners(InmateTransfer transfer) {
        List<User> commissioners = users.findByRoleInAndStatus(Set.of("Commissioner"), "active");
        for (User commissioner : commissioners) {
            if (commissioner.getEmail() == null || commissioner.getEmail().isBlank()) continue;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(commissioner.getEmail());
            message.setSubject("Inmate transfer request requires review");
            message.setText("Hello " + commissioner.getFullName() + ",\n\n"
                    + "A new inmate transfer request is awaiting your review.\n\n"
                    + "Inmate: " + transfer.getInmate().getFirstName() + " " + transfer.getInmate().getLastName() + "\n"
                    + "Source prison: " + transfer.getSourcePrison().getName() + "\n"
                    + "Destination prison: " + (transfer.getDestinationPrison() == null
                            ? "Not specified" : transfer.getDestinationPrison().getName()) + "\n"
                    + "Reason: " + transfer.getReason() + "\n"
                    + "Request ID: " + transfer.getPublicUuid() + "\n\n"
                    + "Sign in to the prison management system to review the request.");
            try {
                mailSender.send(message);
            } catch (MailException exception) {
                LOGGER.warn("Transfer request email delivery failed for commissioner {} ({})",
                        commissioner.getId(), exception.getClass().getName());
            }
        }
    }
}
