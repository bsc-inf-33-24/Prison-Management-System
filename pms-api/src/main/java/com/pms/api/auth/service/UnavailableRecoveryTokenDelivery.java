package com.pms.api.auth.service;

import org.springframework.stereotype.Component;

@Component
public class UnavailableRecoveryTokenDelivery implements RecoveryTokenDelivery {
    @Override public void deliver(String username, String rawToken) {
        throw new RecoveryDeliveryUnavailableException();
    }
    public static class RecoveryDeliveryUnavailableException extends RuntimeException { }
}
