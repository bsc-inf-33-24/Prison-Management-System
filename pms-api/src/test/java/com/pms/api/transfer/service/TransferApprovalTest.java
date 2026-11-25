package com.pms.api.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.api.cell.service.CellService;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.transfer.dto.ChangeTransferStatusRequest;
import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import com.pms.api.transfer.repository.InmateTransferRepository;
import com.pms.api.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TransferApprovalTest {

    private final InmateTransferRepository transfers = mock(InmateTransferRepository.class);
    private final InmateRepository inmates = mock(InmateRepository.class);
    private final PrisonRepository prisons = mock(PrisonRepository.class);
    private final CellService cells = mock(CellService.class);
    private final PaginationProperties pagination = mock(PaginationProperties.class);
    private final PrisonAccessService prisonAccess = mock(PrisonAccessService.class);
    private final TransferService service =
            new TransferService(transfers, inmates, prisons, cells, pagination, prisonAccess);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvalAndConfirmationAreSeparateSteps() {
        Prison source = new Prison("SOURCE", "Source Prison", null);
        Prison destination = new Prison("DEST", "Destination Prison", null);
        setId(source, 1L);
        setId(destination, 2L);
        Inmate inmate = new Inmate("Alex", "Example", LocalDate.of(1990, 1, 15),
                "Other", LocalDate.of(2026, 1, 10), "Test offence",
                LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 9));
        inmate.assignPrison(source);
        InmateTransfer transfer = new InmateTransfer(inmate, source, destination, "Test transfer", "source-officer");
        UUID transferId = UUID.randomUUID();
        when(transfers.findByPublicUuidForUpdate(transferId)).thenReturn(java.util.Optional.of(transfer));
        User commissioner = new User("Commissioner", "commissioner", "password", "Commissioner", java.util.Set.of());
        User receivingOfficer = new User("Receiving Officer", "receiving-officer", "password", "Records Officer", java.util.Set.of());
        when(prisonAccess.currentUser()).thenReturn(commissioner, receivingOfficer);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("commissioner", "", List.of()));
        var approval = service.changeStatus(transferId,
                new ChangeTransferStatusRequest(TransferStatus.APPROVED, null));

        assertEquals(TransferStatus.APPROVED, approval.status());
        assertSame(source, inmate.getCurrentPrison());
        assertEquals("commissioner", transfer.getDecidedBy());
        assertNotNull(transfer.getDecidedAt());
        org.junit.jupiter.api.Assertions.assertNull(transfer.getReceivedAt());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("receiving-officer", "", List.of()));
        var confirmation = service.changeStatus(transferId,
                new ChangeTransferStatusRequest(TransferStatus.CONFIRMED, null));

        assertEquals(TransferStatus.CONFIRMED, confirmation.status());
        assertSame(destination, inmate.getCurrentPrison());
        assertEquals("receiving-officer", transfer.getReceivedBy());
        assertNotNull(transfer.getReceivedAt());
        verify(prisonAccess).requireTransfer(transferId, true);
        org.mockito.Mockito.verifyNoInteractions(cells);
    }

    private static void setId(Prison prison, Long id) {
        try {
            var field = Prison.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(prison, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
