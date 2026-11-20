package com.pms.api.auth.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, String refreshToken, Instant refreshTokenExpiresAt) { }

