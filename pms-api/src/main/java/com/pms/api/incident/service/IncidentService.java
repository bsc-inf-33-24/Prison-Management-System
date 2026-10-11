package com.pms.api.incident.service;

import com.pms.api.common.audit.Audited;
import com.pms.api.incident.dto.IncidentHistoryItemResponse;
import com.pms.api.incident.dto.IncidentResponse;
import com.pms.api.incident.dto.LogIncidentRequest;
import com.pms.api.incident.dto.RecordOutcomeRequest;
import com.pms.api.incident.entity.Incident;
import com.pms.api.incident.entity.IncidentInmate;
import com.pms.api.incident.repository.IncidentInmateRepository;
import com.pms.api.incident.repository.IncidentRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentInmateRepository incidentInmateRepository;
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;

    public IncidentService(IncidentRepository incidentRepository,
                            IncidentInmateRepository incidentInmateRepository,
                            InmateRepository inmateRepository,
                            UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.incidentInmateRepository = incidentInmateRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
    }

    // @Audited marks this as an action worth recording in the system's audit
    // trail (see com.pms.api.common.audit.Audited). NOTE: as of this change,
    // @Audited is still just a marker - nothing in the codebase reads it or
    // writes to the AuditLog table (V8). That gap is pre-existing and is not
    // something this change invents; see the handover notes for Issue #27.
    @Audited
    @Transactional
    public IncidentResponse logIncident(LogIncidentRequest request, String reportingUsername) {
        // Reject the same inmate listed twice (e.g. inmate 1 appearing in
        // two participant entries) before touching the database.
        Map<Long, String> roleByInmateId = new LinkedHashMap<>();
        for (LogIncidentRequest.Participant participant : request.participants()) {
            // @Valid cascades into each element's own field constraints, but
            // it doesn't fail on a null element in the list itself (e.g. a
            // client sending [{"inmateId":1}, null]) - check explicitly.
            if (participant == null) {
                throw new InvalidParticipantException();
            }
            if (roleByInmateId.containsKey(participant.inmateId())) {
                throw new DuplicateInmateIdsException();
            }
            roleByInmateId.put(participant.inmateId(), participant.roleInIncident());
        }

        List<Inmate> inmates = inmateRepository.findAllById(roleByInmateId.keySet());
        if (inmates.size() != roleByInmateId.size()) {
            // Some of the requested inmate IDs don't exist.
            throw new IncidentInmateNotFoundException();
        }

        // reported_by is plain text in the schema (no foreign key to users),
        // but we still resolve it from the authenticated JWT identity rather
        // than trusting client input, the same way the rest of the app
        // derives "who did this" from the logged-in principal.
        User reportingOfficer = userRepository.findByUsername(reportingUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));

        Incident incident = Incident.logNew(request.incidentDate(), request.description(), reportingOfficer.getFullName());
        // Save (and therefore flush) the incident first so its IDENTITY
        // column is populated - IncidentInmate's composite key needs the
        // real incident_id, which doesn't exist until this insert happens.
        incident = incidentRepository.save(incident);

        for (Inmate inmate : inmates) {
            incidentInmateRepository.save(
                    new IncidentInmate(incident, inmate, roleByInmateId.get(inmate.getId())));
        }

        return toResponse(incident, incidentInmateRepository.findByIncidentId(incident.getId()));
    }

    @Audited
    @Transactional
    public IncidentResponse recordOutcome(Long incidentId, RecordOutcomeRequest request) {
        // Confirms the incident exists at all, so we can tell "doesn't
        // exist" (404) apart from "exists but already resolved" (409).
        incidentRepository.findById(incidentId)
                .orElseThrow(IncidentNotFoundException::new);

        // Write-once, concurrency-safe: this only updates a row that is
        // still "open" right now. A second call - whether it arrives after
        // the first finished, or races it - either sees status already
        // 'resolved' or loses the race at the database level, and either
        // way gets 0 rows updated here.
        int updated = incidentRepository.recordOutcomeIfOpen(
                incidentId, request.outcome(), Incident.STATUS_RESOLVED, Incident.STATUS_OPEN);
        if (updated == 0) {
            throw new OutcomeAlreadyRecordedException();
        }

        Incident updatedIncident = incidentRepository.findById(incidentId)
                .orElseThrow(IncidentNotFoundException::new);
        return toResponse(updatedIncident, incidentInmateRepository.findByIncidentId(incidentId));
    }

    @Transactional(readOnly = true)
    public List<IncidentHistoryItemResponse> getHistory(Long inmateId) {
        if (!inmateRepository.existsById(inmateId)) {
            throw new IncidentInmateNotFoundException();
        }
        return incidentInmateRepository.findByInmateIdOrderByIncidentDateDesc(inmateId).stream()
                .map(participant -> new IncidentHistoryItemResponse(
                        participant.getIncident().getId(),
                        participant.getIncident().getIncidentDate(),
                        participant.getIncident().getDescription(),
                        participant.getIncident().getStatus(),
                        participant.getIncident().getOutcome(),
                        participant.getRoleInIncident()))
                .toList();
    }

    private IncidentResponse toResponse(Incident incident, List<IncidentInmate> participants) {
        List<IncidentResponse.ParticipantResponse> participantResponses = participants.stream()
                .map(p -> new IncidentResponse.ParticipantResponse(p.getInmate().getId(), p.getRoleInIncident()))
                .toList();
        return new IncidentResponse(
                incident.getId(), incident.getIncidentDate(), incident.getDescription(),
                incident.getStatus(), incident.getOutcome(), incident.getReportedBy(), participantResponses);
    }

    // Nested exceptions, same style as UserService / InmateService - each
    // one is caught by name in ApiExceptionHandler.
    public static class IncidentNotFoundException extends RuntimeException { }
    public static class IncidentInmateNotFoundException extends RuntimeException { }
    public static class DuplicateInmateIdsException extends RuntimeException { }
    public static class InvalidParticipantException extends RuntimeException { }
    public static class OutcomeAlreadyRecordedException extends RuntimeException { }
}
