package com.pms.api.auth.service;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.recovery.email.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableRecoveryTokenDelivery implements RecoveryTokenDelivery {
    @Override public void deliver(Optional<String> recipient, String rawToken) {
        throw new RecoveryDeliveryUnavailableException();
    }
    public static class RecoveryDeliveryUnavailableException extends RuntimeException {
        public RecoveryDeliveryUnavailableException() { }

        public RecoveryDeliveryUnavailableException(Throwable cause) {
            super(cause);
        }
    }
}
