package com.pms.api.inmate.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.inmates.court-reminder.email.enabled", havingValue = "true")
public class CourtDateEmailDelivery {
    private static final Logger LOG = LoggerFactory.getLogger(CourtDateEmailDelivery.class);
    private final JavaMailSender mailSender;
    private final UserRepository users;
    private final String from;
    public CourtDateEmailDelivery(JavaMailSender mailSender, UserRepository users,
            @Value("${app.inmates.court-reminder.email.from:}") String from) {
        this.mailSender = mailSender; this.users = users; this.from = from;
        if (from.isBlank()) throw new IllegalStateException("Court reminder email requires a sender address.");
    }
    public boolean send(Inmate inmate, int daysBefore) {
        String text = "Court is scheduled for " + inmate.getFirstName() + " " + inmate.getLastName()
                + " on " + inmate.getExpectedCourtDate() + " (" + daysBefore + " days from today).";
        boolean delivered = false;
        for (User user : users.findByRoleInAndStatus(Set.of("Records Officer", "Duty Officer"), User.ACTIVE)) {
            if (user.getEmail() == null || user.getEmail().isBlank()) continue;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from); message.setTo(user.getEmail());
            message.setSubject("Upcoming inmate court date"); message.setText(text);
            try { mailSender.send(message); delivered = true; }
            catch (MailException ex) { LOG.warn("Court reminder email failed for user {} ({})", user.getId(), ex.getClass().getSimpleName()); }
        }
        return delivered;
    }
}
