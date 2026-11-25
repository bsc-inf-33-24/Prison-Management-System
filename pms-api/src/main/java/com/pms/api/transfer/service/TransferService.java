package com.pms.api.transfer.service;

import com.pms.api.cell.service.CellService;
import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.inmate.entity.CustodyStay;
import com.pms.api.inmate.repository.CustodyStayRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.transfer.dto.ChangeTransferStatusRequest;
import com.pms.api.transfer.dto.ApproveTransferRequest;
import com.pms.api.transfer.dto.ConfirmTransferReceiptRequest;
import com.pms.api.transfer.dto.CreateTransferRequest;
import com.pms.api.transfer.dto.TransferResponse;
import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import com.pms.api.transfer.repository.InmateTransferRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.user.entity.User;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CustodyStayRepository custodyStays;
    private TransferRequestEmailService transferRequestEmailService;
    @Autowired
    public TransferService(InmateTransferRepository transfers,InmateRepository inmates,PrisonRepository prisons,
                           CellService cells,PaginationProperties pagination,PrisonAccessService prisonAccess,
                           CustodyStayRepository custodyStays) {
        this.transfers=transfers; this.inmates=inmates; this.prisons=prisons; this.cells=cells; this.pagination=pagination; this.prisonAccess=prisonAccess; this.custodyStays=custodyStays;
    }

    public TransferService(InmateTransferRepository transfers, InmateRepository inmates, PrisonRepository prisons,
                           CellService cells, PaginationProperties pagination, PrisonAccessService prisonAccess) {
        this(transfers, inmates, prisons, cells, pagination, prisonAccess, null);
    }

    @Autowired(required = false)
    public void setTransferRequestEmailService(TransferRequestEmailService transferRequestEmailService) {
        this.transferRequestEmailService = transferRequestEmailService;
    }

    @Audited @Transactional
    public TransferResponse create(CreateTransferRequest request) {
        Inmate inmate=inmates.findByPublicUuid(request.inmateId()).orElseThrow(InmateNotFoundException::new);
        prisonAccess.requireInmate(request.inmateId());
        User creator=prisonAccess.currentUser();
        boolean systemWideTransferOfficer="Transfer Officer".equals(creator.getRole());
        boolean commissioner="Commissioner".equals(creator.getRole());
        if (!commissioner && !systemWideTransferOfficer && request.sourcePrisonId()==null)
            throw new MissingTransferPrisonsException();
        Prison source=request.sourcePrisonId()==null?inmate.getCurrentPrison():prisons.findByPublicUuid(request.sourcePrisonId()).orElseThrow(PrisonNotFoundException::new);
        if (source==null) throw new InmatePrisonNotAssignedException();
        Prison destination=request.destinationPrisonId()==null?null:prisons.findByPublicUuid(request.destinationPrisonId()).orElseThrow(PrisonNotFoundException::new);
        if (!commissioner) prisonAccess.requirePrison(source.getPublicUuid());
        if (inmate.getCurrentPrison()==null || !inmate.getCurrentPrison().getId().equals(source.getId())) throw new InmatePrisonNotAssignedException();
        if (!source.isActive() || (destination!=null && !destination.isActive())) throw new InactivePrisonException();
        if (destination!=null && source.getId().equals(destination.getId())) throw new SamePrisonException();
        if (transfers.existsByInmate_IdAndStatusIn(inmate.getId(),ACTIVE)) throw new ActiveTransferExistsException();
        try {
            InmateTransfer transfer=transfers.saveAndFlush(new InmateTransfer(inmate,source,destination,request.reason().trim(),actor()));
            if (transferRequestEmailService != null) transferRequestEmailService.notifyCommissioners(transfer);
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
    public TransferResponse changeStatus(UUID id, ChangeTransferStatusRequest request) {
        InmateTransfer transfer=getLocked(id);
        User user=prisonAccess.currentUser();
        if (request.destinationPrisonId()!=null && request.status()!=TransferStatus.APPROVED)
            throw new InvalidTransferStatusException();
        if ("Commissioner".equals(user.getRole()) || "Transfer Officer".equals(user.getRole())) {
            if (request.status()==TransferStatus.APPROVED) {
                Prison destination=request.destinationPrisonId()==null
                        ?transfer.getDestinationPrison()
                        :prisons.findByPublicUuid(request.destinationPrisonId()).orElseThrow(PrisonNotFoundException::new);
                if (destination==null) throw new MissingTransferPrisonsException();
                if (!destination.isActive()) throw new InactivePrisonException();
                if (destination.getId().equals(transfer.getSourcePrison().getId())) throw new SamePrisonException();
                transfer.approve(actor(), null, destination);
            }
            else if (request.status()==TransferStatus.REJECTED) transfer.reject(actor(), null);
            else throw new InvalidTransferStatusException();
        } else if ("Records Officer".equals(user.getRole()) && request.status()==TransferStatus.CONFIRMED) {
            return confirmReceipt(id, transfer, request.cellId());
        } else if (request.status()==TransferStatus.CANCELLED && "Records Officer".equals(user.getRole())) {
            prisonAccess.requireTransfer(id, false);
            transfer.cancel(actor(), null);
        } else throw new InvalidTransferStatusException();
        return TransferResponse.from(transfer);
    }

    @Audited
    @Transactional
    public TransferResponse confirmReceipt(UUID id, ConfirmTransferReceiptRequest request) {
        InmateTransfer transfer=getLocked(id);
        User user=prisonAccess.currentUser();
        if (!"Records Officer".equals(user.getRole())) throw new InvalidTransferStatusException();
        return confirmReceipt(id, transfer, request.cellId());
    }

    private TransferResponse confirmReceipt(UUID id, InmateTransfer transfer, UUID cellId) {
        if (transfer.getDestinationPrison()==null) throw new MissingTransferPrisonsException();
        if (cellId==null) throw new MissingDestinationCellException();
        prisonAccess.requireTransfer(id, true);

        Inmate inmate=transfer.getInmate();
        transfer.confirm(actor(), cellId);
        inmate.assignPrison(transfer.getDestinationPrison());
        cells.releaseCurrentAllocation(inmate.getId());
        cells.allocate(inmate.getPublicUuid(), new AllocateCellRequest(cellId));

        if (custodyStays!=null) {
            CustodyStay stay=custodyStays.findFirstByInmate_IdAndEndedAtIsNullOrderByIdDesc(inmate.getId()).orElseThrow(InmateHasNoCellException::new);
            stay.transferTo(transfer.getDestinationPrison());
            custodyStays.saveAndFlush(stay);
        }
        return TransferResponse.from(transfer);
    }

    /** Kept for source compatibility with existing internal callers; HTTP mutations use changeStatus. */
    @Deprecated
    public TransferResponse approve(UUID id, ApproveTransferRequest request) {
        InmateTransfer transfer=getLocked(id);
        transfer.approve(actor(), clean(request==null?null:request.note()));
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
    public static class MissingTransferPrisonsException extends RuntimeException { }
    public static class InvalidTransferStatusException extends RuntimeException { }
    public static class MissingDestinationCellException extends RuntimeException { }
}
