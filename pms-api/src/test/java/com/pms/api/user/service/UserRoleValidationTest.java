package com.pms.api.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.pms.api.user.entity.User;
import org.junit.jupiter.api.Test;

class UserRoleValidationTest {

    @Test
    void commissionerRoleIsAllowedWithoutPrisonAssignment() {
        assertThat(User.ALLOWED_ROLES).contains("Commissioner");
        assertThatCode(() -> UserService.validateRole("Commissioner")).doesNotThrowAnyException();
        assertThat(UserService.toAuthorityRole("Commissioner")).isEqualTo("COMMISSIONER");
    }
}
