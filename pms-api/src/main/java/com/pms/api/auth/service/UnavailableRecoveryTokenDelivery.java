package com.pms.api.auth.service;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UnavailableRecoveryTokenDelivery implements RecoveryTokenDelivery {
    @Override public void deliver(Optional<String> recipient, String rawToken) {
        throw new RecoveryDeliveryUnavailableException();
    }
    public static class RecoveryDeliveryUnavailableException extends RuntimeException { }
}
