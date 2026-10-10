package com.pms.api.incident.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.incident.dto.IncidentHistoryItemResponse;
import com.pms.api.incident.dto.IncidentResponse;
import com.pms.api.incident.dto.LogIncidentRequest;
import com.pms.api.incident.dto.RecordOutcomeRequest;
import com.pms.api.incident.entity.Incident;
import com.pms.api.incident.repository.IncidentRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.common.audit.Audited;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;

    public IncidentService(IncidentRepository incidentRepository,
                            InmateRepository inmateRepository,
                            UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
    }

    // @Audited marks this as an action worth recording in the system's
    // audit trail (see com.pms.api.common.audit.Audited). The controller
    // only gives us the logged-in user's username (from the JWT) — we
    // resolve that to the real User entity here, the same way AuthService
    // does for changePassword.
    @Audited
    @Transactional
    public IncidentResponse logIncident(LogIncidentRequest request, String loggedByUsername) {
        // Reject duplicate inmate IDs up front — e.g. [1, 1, 2] — before we
        // even touch the database.
        Set<Long> uniqueIds = new LinkedHashSet<>(request.inmateIds());
        if (uniqueIds.size() != request.inmateIds().size()) {
            throw new DuplicateInmateIdsException();
        }

        List<Inmate> inmates = inmateRepository.findAllById(uniqueIds);
        if (inmates.size() != uniqueIds.size()) {
            // Some of the requested inmate IDs don't exist.
            throw new IncidentInmateNotFoundException();
        }

        User loggedBy = userRepository.findByUsername(loggedByUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));

        Incident incident = Incident.logNew(
                request.incidentDate(), request.description(), loggedBy, new LinkedHashSet<>(inmates));

        return toResponse(incidentRepository.save(incident));
    }

    @Audited
    @Transactional
    public IncidentResponse recordOutcome(Long incidentId, RecordOutcomeRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(IncidentNotFoundException::new);

        // Write-once: a second PATCH on an already-resolved incident is
        // rejected rather than silently overwriting the first outcome.
        if (incident.getOutcome() != null) {
            throw new OutcomeAlreadyRecordedException();
        }

        incident.setOutcome(request.outcome());
        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentHistoryItemResponse> getHistory(Long inmateId) {
        if (!inmateRepository.existsById(inmateId)) {
            throw new IncidentInmateNotFoundException();
        }
        return incidentRepository.findByInmateId(inmateId).stream()
                .map(incident -> new IncidentHistoryItemResponse(
                        incident.getId(), incident.getIncidentDate(), incident.getDescription(), incident.getOutcome()))
                .toList();
    }

    private IncidentResponse toResponse(Incident incident) {
        List<Long> inmateIds = incident.getInmates().stream().map(Inmate::getId).toList();
        return new IncidentResponse(
                incident.getId(), inmateIds, incident.getIncidentDate(), incident.getDescription(),
                incident.getOutcome(), incident.getLoggedBy().getId(), incident.getCreatedAt());
    }

    // Nested exceptions, same style as UserService / InmateService — each
    // one is caught by name in ApiExceptionHandler.
    public static class IncidentNotFoundException extends RuntimeException { }
    public static class IncidentInmateNotFoundException extends RuntimeException { }
    public static class DuplicateInmateIdsException extends RuntimeException { }
    public static class OutcomeAlreadyRecordedException extends RuntimeException { }
}