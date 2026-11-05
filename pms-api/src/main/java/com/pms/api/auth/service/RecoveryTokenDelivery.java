package com.pms.api.auth.service;

import java.util.Optional;

/** Integration boundary for the recovery channel. Empty recipients must not trigger delivery; raw codes must never be logged. */
public interface RecoveryTokenDelivery {
    void deliver(Optional<String> recipient, String recoveryCode);
}
