package com.pms.api.auth.service;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 16 || password.isBlank()) {
            throw new WeakPasswordException();
        }
        if (!password.matches(".*\\p{javaDigit}.*") || !password.matches(".*[^\\p{L}\\p{N}\\s].*")) {
            throw new WeakPasswordException();
        }
    }
    public static class WeakPasswordException extends RuntimeException { }
}
