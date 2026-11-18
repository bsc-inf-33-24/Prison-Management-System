package com.pms.api.inmate.service;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.repository.CellAllocationRepository;
import com.pms.api.cell.service.CellService;
import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.dto.CustodyStayResponse;
import com.pms.api.inmate.dto.ReadmissionRequest;
import com.pms.api.inmate.entity.CustodyStay;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.CustodyStayRepository;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.notification.service.NotificationService;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.PrisonAccessService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadmissionService {
    private final InmateRepository inmates;
    private final CustodyStayRepository stays;
    private final CellAllocationRepository allocations;
    private final PrisonRepository prisons;
    private final PrisonAccessService access;
    private final CellService cells;
    private final NotificationService notifications;

    public ReadmissionService(InmateRepository inmates, CustodyStayRepository stays,
            CellAllocationRepository allocations, PrisonRepository prisons, PrisonAccessService access,
            CellService cells, NotificationService notifications) {
        this.inmates = inmates; this.stays = stays; this.allocations = allocations;
        this.prisons = prisons; this.access = access; this.cells = cells; this.notifications = notifications;
    }

    @Audited @Transactional
    public CustodyStayResponse readmit(UUID prisonerId, ReadmissionRequest request) {
        Inmate inmate = inmates.findByPublicUuid(prisonerId).orElseThrow(InmateService.InmateNotFoundException::new);
        access.effectivePrison(null);
        if (allocations.findByInmate_IdAndReleasedAtIsNull(inmate.getId()).isPresent()
                || stays.findFirstByInmate_IdAndEndedAtIsNullOrderByIdDesc(inmate.getId()).isPresent()) {
            throw new ActiveCustodyException();
        }
        UUID prisonId = access.effectivePrison(null);
        Prison prison = prisons.findByPublicUuid(prisonId).orElseThrow(InmateService.PrisonNotFoundException::new);
        if (!prison.isActive()) throw new InmateService.PrisonInactiveException();
        inmate.assignPrison(prison);
        inmate = inmates.saveAndFlush(inmate);
        CustodyStay stay = stays.saveAndFlush(new CustodyStay(inmate, prison, request.admissionDate()));
        cells.allocate(prisonerId, new AllocateCellRequest(request.cellId()));
        notifications.notifyRoles("INMATE_READMITTED", "Inmate readmitted",
                "An existing inmate has been readmitted and assigned to a cell.", prisonerId,
                Set.of("Records Officer", "Duty Officer"));
        return CustodyStayResponse.from(stay);
    }

    @Audited @Transactional
    public CustodyStayResponse discharge(UUID prisonerId) {
        Inmate inmate = inmates.findByPublicUuid(prisonerId).orElseThrow(InmateService.InmateNotFoundException::new);
        access.requireInmate(prisonerId);
        CustodyStay stay = stays.findFirstByInmate_IdAndEndedAtIsNullOrderByIdDesc(inmate.getId())
                .orElseThrow(NoActiveCustodyException::new);
        access.requirePrison(stay.getPrison().getPublicUuid());
        Instant dischargedAt = Instant.now();
        stay.end(dischargedAt);
        stays.saveAndFlush(stay);
        if (allocations.releaseByInmateId(inmate.getId(), dischargedAt) == 0) {
            throw new NoActiveCustodyException();
        }
        notifications.notifyRoles("INMATE_DISCHARGED", "Inmate discharged",
                "An inmate has been discharged from custody.", prisonerId,
                Set.of("Records Officer", "Duty Officer"));
        return CustodyStayResponse.from(stay);
    }
    public static class ActiveCustodyException extends RuntimeException { }
    public static class NoActiveCustodyException extends RuntimeException { }
}
