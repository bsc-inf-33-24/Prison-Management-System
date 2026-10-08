
package com.pms.api.auth.dto;

public record AuthResponse(String token, String tokenType, UserInfo user) {

    public record UserInfo(Long id, String fullName, String role) { }
}

