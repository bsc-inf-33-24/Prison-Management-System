package com.pms.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.api.cell.repository.CellRepository;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.repository.BlockRepository;
import com.pms.api.transfer.repository.InmateTransferRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TransferOfficerAccessTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PrisonAccessService access = new PrisonAccessService(
            users, mock(BlockRepository.class), mock(InmateRepository.class),
            mock(CellRepository.class), mock(InmateTransferRepository.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferOfficerHasUnrestrictedPrisonScopeWithoutPrisonAssignment() {
        User user = new User("Transfer Officer", "transfer-officer", "password",
                "Transfer Officer", Set.of());
        when(users.findByUsername("transfer-officer")).thenReturn(java.util.Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("transfer-officer", "", Set.of()));

        assertTrue(User.ALLOWED_ROLES.contains("Transfer Officer"));
        assertTrue(access.isGlobalAdmin());
        assertNull(access.effectivePrison(null));
        UUID requestedPrisonId = UUID.randomUUID();
        assertEquals(requestedPrisonId, access.effectivePrison(requestedPrisonId));
    }
}
