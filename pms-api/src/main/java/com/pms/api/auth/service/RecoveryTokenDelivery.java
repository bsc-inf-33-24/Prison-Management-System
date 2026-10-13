package com.pms.api.auth.service;

/** Integration boundary for the configured recovery channel. Raw tokens must never be logged. */
public interface RecoveryTokenDelivery {
    void deliver(String username, String rawToken);
}
