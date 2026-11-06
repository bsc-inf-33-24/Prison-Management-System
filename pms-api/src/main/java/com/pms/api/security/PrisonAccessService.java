package com.pms.api.security;

import com.pms.api.cell.entity.Cell;
import com.pms.api.cell.repository.CellRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.prison.repository.BlockRepository;
import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.repository.InmateTransferRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Enforces the prison boundary using the authenticated account's current database assignment. */
@Service("prisonAccess")
public class PrisonAccessService {
    private final UserRepository users;
    private final BlockRepository blocks;
    private final InmateRepository inmates;
    private final CellRepository cells;
    private final InmateTransferRepository transfers;

    public PrisonAccessService(UserRepository users, BlockRepository blocks, InmateRepository inmates,
                               CellRepository cells, InmateTransferRepository transfers) {
        this.users=users; this.blocks=blocks; this.inmates=inmates; this.cells=cells; this.transfers=transfers;
    }

    public boolean isGlobalAdmin() { return "Super Admin".equals(currentUser().getRole()); }

    /** Null means an unrestricted Super Admin; otherwise it is the account's prison UUID. */
    public UUID effectivePrison(UUID requestedPrisonId) {
        User user=currentUser();
        if ("Super Admin".equals(user.getRole())) return requestedPrisonId;
        if (user.getPrison()==null || !user.getPrison().isActive()) deny();
        UUID assigned=user.getPrison()==null?null:user.getPrison().getPublicUuid();
        if (assigned==null || (requestedPrisonId!=null && !assigned.equals(requestedPrisonId))) deny();
        return assigned;
    }

    public void requirePrison(UUID prisonId) {
        if (!isGlobalAdmin() && !effectivePrison(null).equals(prisonId)) deny();
    }

    public void requireInmate(UUID inmateId) {
        if (isGlobalAdmin()) return;
        Inmate inmate=inmates.findByPublicUuid(inmateId).orElseThrow(PrisonAccessService::denied);
        if (inmate.getCurrentPrison()==null) deny();
        requirePrison(inmate.getCurrentPrison().getPublicUuid());
    }

    public void requireCell(UUID cellId) {
        if (isGlobalAdmin()) return;
        Cell cell=cells.findByPublicUuid(cellId).orElseThrow(PrisonAccessService::denied);
        if (cell.getBlock()==null) deny();
        requirePrison(cell.getBlock().getPrison().getPublicUuid());
    }

    public void requireBlock(UUID blockId) {
        if (isGlobalAdmin()) return;
        var block=blocks.findByPublicUuid(blockId).orElseThrow(PrisonAccessService::denied);
        requirePrison(block.getPrison().getPublicUuid());
    }

    public void requireTransfer(UUID transferId, boolean destinationSide) {
        if (isGlobalAdmin()) return;
        InmateTransfer transfer=transfers.findByPublicUuid(transferId).orElseThrow(PrisonAccessService::denied);
        UUID prisonId=(destinationSide?transfer.getDestinationPrison():transfer.getSourcePrison()).getPublicUuid();
        requirePrison(prisonId);
    }

    public User currentUser() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null || !authentication.isAuthenticated() || authentication.getName()==null) deny();
        return users.findByUsername(authentication.getName()).orElseThrow(PrisonAccessService::denied);
    }

    private static AccessDeniedException denied() { return new AccessDeniedException("Access is restricted to the assigned prison."); }
    private static void deny() { throw denied(); }
}
