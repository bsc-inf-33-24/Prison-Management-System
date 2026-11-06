package com.pms.api.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.pms.api.transfer.dto.ApproveTransferRequest;
import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import com.pms.api.transfer.repository.InmateTransferRepository;
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
    void approvalCompletesTransferAndMovesInmateIntoSelectedDestinationCell() {
        String actor = "receiving-officer";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "", List.of()));
        Prison source = new Prison("SOURCE", "Source Prison", null);
        Prison destination = new Prison("DEST", "Destination Prison", null);
        Inmate inmate = new Inmate("Alex", "Example", LocalDate.of(1990, 1, 15),
                "Other", LocalDate.of(2026, 1, 10), "Test offence",
                LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 9));
        inmate.assignPrison(source);
        InmateTransfer transfer = new InmateTransfer(inmate, source, destination, "Test transfer", "source-officer");
        UUID transferId = UUID.randomUUID();
        UUID destinationCellId = UUID.randomUUID();
        when(transfers.findByPublicUuidForUpdate(transferId)).thenReturn(java.util.Optional.of(transfer));
        when(cells.releaseCurrentAllocation(any())).thenReturn(1);

        var response = service.approve(transferId, new ApproveTransferRequest(destinationCellId, "Approved"));

        assertEquals(TransferStatus.RECEIVED, response.status());
        assertSame(destination, inmate.getCurrentPrison());
        assertEquals(actor, transfer.getDecidedBy());
        assertEquals(actor, transfer.getDispatchedBy());
        assertEquals(actor, transfer.getReceivedBy());
        assertNotNull(transfer.getDecidedAt());
        assertNotNull(transfer.getDispatchedAt());
        assertNotNull(transfer.getReceivedAt());
        verify(prisonAccess).requireTransfer(transferId, true);
        verify(cells).releaseCurrentAllocation(inmate.getId());
        verify(cells).allocate(eq(inmate.getPublicUuid()), eq(new AllocateCellRequest(destinationCellId)));
    }
}
