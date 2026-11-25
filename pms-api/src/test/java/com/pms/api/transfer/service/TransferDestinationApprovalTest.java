package com.pms.api.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.service.CellService;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.transfer.dto.ChangeTransferStatusRequest;
import com.pms.api.transfer.dto.ConfirmTransferReceiptRequest;
import com.pms.api.transfer.dto.CreateTransferRequest;
import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import com.pms.api.transfer.repository.InmateTransferRepository;
import com.pms.api.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TransferDestinationApprovalTest {
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
    void transferRequestCanOmitDestination() {
        Prison source = prison(1L, UUID.randomUUID(), "Source Prison");
        Inmate inmate = inmate(source);
        UUID inmateId = inmate.getPublicUuid();
        when(inmates.findByPublicUuid(inmateId)).thenReturn(java.util.Optional.of(inmate));
        when(prisonAccess.currentUser()).thenReturn(user("Records Officer"));
        when(prisons.findByPublicUuid(source.getPublicUuid())).thenReturn(java.util.Optional.of(source));
        when(transfers.saveAndFlush(any(InmateTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        authenticate("records-officer");

        var result = service.create(new CreateTransferRequest(inmateId, source.getPublicUuid(), null, "Operational need"));

        assertNull(result.destinationPrisonId());
    }

    @Test
    void approvalKeepsPreviouslySelectedDestinationWhenOmitted() {
        Prison source = prison(1L, UUID.randomUUID(), "Source Prison");
        Prison destination = prison(2L, UUID.randomUUID(), "Destination Prison");
        InmateTransfer transfer = new InmateTransfer(inmate(source), source, destination, "Operational need", "requester");
        UUID transferId = UUID.randomUUID();
        when(transfers.findByPublicUuidForUpdate(transferId)).thenReturn(java.util.Optional.of(transfer));
        when(prisonAccess.currentUser()).thenReturn(user("Transfer Officer"));
        authenticate("transfer-officer");

        var result = service.changeStatus(transferId,
                new ChangeTransferStatusRequest(TransferStatus.APPROVED, null));

        assertEquals(TransferStatus.APPROVED, result.status());
        assertEquals(destination.getPublicUuid(), result.destinationPrisonId());
    }

    @Test
    void approvalRequiresDestinationWhenRequestDidNotSpecifyOne() {
        Prison source = prison(1L, UUID.randomUUID(), "Source Prison");
        InmateTransfer transfer = new InmateTransfer(inmate(source), source, null, "Operational need", "requester");
        UUID transferId = UUID.randomUUID();
        when(transfers.findByPublicUuidForUpdate(transferId)).thenReturn(java.util.Optional.of(transfer));
        when(prisonAccess.currentUser()).thenReturn(user("Transfer Officer"));
        authenticate("transfer-officer");

        assertThrows(TransferService.MissingTransferPrisonsException.class,
                () -> service.changeStatus(transferId,
                        new ChangeTransferStatusRequest(TransferStatus.APPROVED, null)));
    }

    @Test
    void approvalCanSetDestinationWhenRequestDidNotSpecifyOne() {
        Prison source = prison(1L, UUID.randomUUID(), "Source Prison");
        Prison destination = prison(2L, UUID.randomUUID(), "Destination Prison");
        InmateTransfer transfer = new InmateTransfer(inmate(source), source, null, "Operational need", "requester");
        UUID transferId = UUID.randomUUID();
        when(transfers.findByPublicUuidForUpdate(transferId)).thenReturn(java.util.Optional.of(transfer));
        when(prisons.findByPublicUuid(destination.getPublicUuid())).thenReturn(java.util.Optional.of(destination));
        when(prisonAccess.currentUser()).thenReturn(user("Transfer Officer"));
        authenticate("transfer-officer");

        var result = service.changeStatus(transferId,
                new ChangeTransferStatusRequest(TransferStatus.APPROVED, destination.getPublicUuid()));

        assertEquals(TransferStatus.APPROVED, result.status());
        assertEquals(destination.getPublicUuid(), result.destinationPrisonId());
    }

    @Test
    void confirmingTransferReleasesOldCellAndAssignsDestinationCell() {
        Prison source = prison(1L, UUID.randomUUID(), "Source Prison");
        Prison destination = prison(2L, UUID.randomUUID(), "Destination Prison");
        Inmate inmate = inmate(source);
        InmateTransfer transfer = new InmateTransfer(inmate, source, destination, "Operational need", "requester");
        transfer.approve("approver", null, destination);
        UUID transferId = UUID.randomUUID();
        UUID cellId = UUID.randomUUID();
        when(transfers.findByPublicUuidForUpdate(transferId)).thenReturn(java.util.Optional.of(transfer));
        when(prisonAccess.currentUser()).thenReturn(user("Records Officer"));
        authenticate("records-officer");

        var result = service.confirmReceipt(transferId, new ConfirmTransferReceiptRequest(cellId));

        assertEquals(TransferStatus.CONFIRMED, result.status());
        assertEquals(destination, inmate.getCurrentPrison());
        assertEquals(cellId, result.receivedCellId());
        verify(cells).releaseCurrentAllocation(inmate.getId());
        verify(cells).allocate(inmate.getPublicUuid(), new AllocateCellRequest(cellId));
        verify(prisonAccess).requireTransfer(transferId, true);
    }

    private static Prison prison(Long id, UUID publicUuid, String name) {
        Prison prison = mock(Prison.class);
        when(prison.getId()).thenReturn(id);
        when(prison.getPublicUuid()).thenReturn(publicUuid);
        when(prison.getName()).thenReturn(name);
        when(prison.isActive()).thenReturn(true);
        return prison;
    }

    private static Inmate inmate(Prison prison) {
        Inmate inmate = new Inmate("Alex", "Example", LocalDate.of(1990, 1, 15),
                "Other", LocalDate.of(2026, 1, 10), "Test offence",
                LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 9));
        inmate.assignPrison(prison);
        return inmate;
    }

    private static User user(String role) {
        return new User("Test User", "test-user", "password", role, Set.of());
    }

    private static void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "", List.of()));
    }
}
