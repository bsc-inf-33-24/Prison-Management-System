package com.pms.api.transfer.service;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.service.CellService;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.transfer.dto.ApproveTransferRequest;
import com.pms.api.transfer.dto.CreateTransferRequest;
import com.pms.api.transfer.dto.TransferDecisionRequest;
import com.pms.api.transfer.dto.TransferResponse;
import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import com.pms.api.transfer.repository.InmateTransferRepository;
import com.pms.api.security.PrisonAccessService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private static final List<TransferStatus> ACTIVE=List.of(TransferStatus.PENDING,TransferStatus.APPROVED,TransferStatus.IN_TRANSIT);
    private final InmateTransferRepository transfers;
    private final InmateRepository inmates;
    private final PrisonRepository prisons;
    private final CellService cells;
    private final PaginationProperties pagination;
    private final PrisonAccessService prisonAccess;
    public TransferService(InmateTransferRepository transfers,InmateRepository inmates,PrisonRepository prisons,
                           CellService cells,PaginationProperties pagination,PrisonAccessService prisonAccess) {
        this.transfers=transfers; this.inmates=inmates; this.prisons=prisons; this.cells=cells; this.pagination=pagination; this.prisonAccess=prisonAccess;
    }

    @Audited @Transactional
    public TransferResponse create(CreateTransferRequest request) {
        Inmate inmate=inmates.findByPublicUuid(request.inmateId()).orElseThrow(InmateNotFoundException::new);
        prisonAccess.requireInmate(request.inmateId());
        Prison source=inmate.getCurrentPrison();
        if (source==null) throw new InmatePrisonNotAssignedException();
        Prison destination=prisons.findByPublicUuid(request.destinationPrisonId()).orElseThrow(PrisonNotFoundException::new);
        if (!source.isActive() || !destination.isActive()) throw new InactivePrisonException();
        if (source.getId().equals(destination.getId())) throw new SamePrisonException();
        if (transfers.existsByInmate_IdAndStatusIn(inmate.getId(),ACTIVE)) throw new ActiveTransferExistsException();
        try {
            InmateTransfer transfer=transfers.saveAndFlush(new InmateTransfer(inmate,source,destination,request.reason().trim(),actor()));
            return TransferResponse.from(transfer);
        } catch (DataIntegrityViolationException exception) { throw new ActiveTransferExistsException(); }
    }

    @Transactional(readOnly=true)
    public PageResponse<TransferResponse> list(TransferStatus status,UUID prisonId,Integer page,Integer size,List<String> sort) {
        UUID effectivePrisonId=prisonAccess.effectivePrison(prisonId);
        Pageable pageable=PageableValidator.create(page,size,sort,Set.of("requestedAt","status","id"),
                Sort.by(Sort.Order.desc("requestedAt")),pagination);
        Specification<InmateTransfer> spec=Specification.unrestricted();
        if (status!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("status"),status));
        if (effectivePrisonId!=null) spec=spec.and((root,q,cb)->cb.or(
                cb.equal(root.get("sourcePrison").get("publicUuid"),effectivePrisonId),
                cb.equal(root.get("destinationPrison").get("publicUuid"),effectivePrisonId)));
        return PageResponse.from(transfers.findAll(spec,pageable).map(TransferResponse::from));
    }

    @Audited
    @Transactional
    public TransferResponse approve(UUID id, ApproveTransferRequest request) {
        prisonAccess.requireTransfer(id, true);
        InmateTransfer transfer=getLocked(id);
        String actor = actor();
        transfer.approve(actor, clean(request.note()));
        if (cells.releaseCurrentAllocation(transfer.getInmate().getId()) == 0) {
            throw new InmateHasNoCellException();
        }
        Inmate inmate = transfer.getInmate();
        inmate.assignPrison(transfer.getDestinationPrison());
        cells.allocate(inmate.getPublicUuid(), new AllocateCellRequest(request.cellId()));
        return TransferResponse.from(transfer);
    }

    @Audited
    @Transactional
    public TransferResponse reject(UUID id, TransferDecisionRequest request) {
        prisonAccess.requireTransfer(id, true);
        InmateTransfer transfer = getLocked(id);
        transfer.reject(actor(), request == null ? null : clean(request.note()));
        return TransferResponse.from(transfer);
    }

    @Audited @Transactional
    public TransferResponse cancel(UUID id,TransferDecisionRequest request) {
        prisonAccess.requireTransfer(id,false);
        InmateTransfer transfer=getLocked(id);
        transfer.cancel(actor(),request==null?null:clean(request.note()));
        return TransferResponse.from(transfer);
    }

    private InmateTransfer getLocked(UUID id) { return transfers.findByPublicUuidForUpdate(id).orElseThrow(TransferNotFoundException::new); }
    private static String actor() { Authentication auth=SecurityContextHolder.getContext().getAuthentication(); return auth==null?"unknown":auth.getName(); }
    private static String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    public static class InmateNotFoundException extends RuntimeException { }
    public static class PrisonNotFoundException extends RuntimeException { }
    public static class InmatePrisonNotAssignedException extends RuntimeException { }
    public static class InactivePrisonException extends RuntimeException { }
    public static class SamePrisonException extends RuntimeException { }
    public static class ActiveTransferExistsException extends RuntimeException { }
    public static class TransferNotFoundException extends RuntimeException { }
    public static class InmateHasNoCellException extends RuntimeException { }
}
