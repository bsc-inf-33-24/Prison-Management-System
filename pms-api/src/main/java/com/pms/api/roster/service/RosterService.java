package com.pms.api.roster.service;

import com.pms.api.notification.service.NotificationService;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.roster.dto.CreateRosterRequest;
import com.pms.api.roster.dto.CreateShiftRequest;
import com.pms.api.roster.dto.RosterResponse;
import com.pms.api.roster.dto.RosterShiftResponse;
import com.pms.api.roster.dto.RosterSummaryResponse;
import com.pms.api.roster.dto.UpdateRosterRequest;
import com.pms.api.roster.dto.UpdateShiftRequest;
import com.pms.api.roster.entity.Roster;
import com.pms.api.roster.entity.RosterShift;
import com.pms.api.roster.entity.RosterStatus;
import com.pms.api.roster.repository.RosterRepository;
import com.pms.api.roster.repository.RosterShiftRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RosterService {

    private static final String ELIGIBLE_OFFICER_ROLE = "Prison Officer";
    private static final Duration MAX_SHIFT_DURATION = Duration.ofHours(24);

    private final RosterRepository rosterRepository;
    private final RosterShiftRepository shiftRepository;
    private final PrisonRepository prisonRepository;
    private final UserRepository userRepository;
    private final PrisonAccessService prisonAccess;
    private final NotificationService notificationService;
    private final ZoneId rosterTimeZone;

    public RosterService(RosterRepository rosterRepository,
                         RosterShiftRepository shiftRepository,
                         PrisonRepository prisonRepository,
                         UserRepository userRepository,
                         PrisonAccessService prisonAccess,
                         NotificationService notificationService,
                         @Value("${app.rosters.time-zone:Africa/Blantyre}") String rosterTimeZone) {
        this.rosterRepository = rosterRepository;
        this.shiftRepository = shiftRepository;
        this.prisonRepository = prisonRepository;
        this.userRepository = userRepository;
        this.prisonAccess = prisonAccess;
        this.notificationService = notificationService;
        this.rosterTimeZone = ZoneId.of(rosterTimeZone);
    }

    @Transactional
    public RosterResponse create(CreateRosterRequest request) {
        validateDateRange(request.periodStart(), request.periodEnd());
        UUID prisonId = prisonAccess.effectivePrison(request.prisonId());
        if (prisonId == null) {
            throw new IllegalArgumentException("prisonId is required for a Super Admin.");
        }
        Prison prison = prisonRepository.findByPublicUuid(prisonId)
                .orElseThrow(PrisonNotFoundException::new);
        if (!prison.isActive()) {
            throw new InactivePrisonException();
        }
        Roster roster = rosterRepository.saveAndFlush(new Roster(
                prison, request.periodStart(), request.periodEnd(), prisonAccess.currentUser()));
        return response(roster);
    }

    @Transactional
    public RosterResponse update(UUID rosterId, UpdateRosterRequest request) {
        Roster roster = findRosterForUpdate(rosterId);
        requireDraft(roster);
        validateDateRange(request.periodStart(), request.periodEnd());
        for (RosterShift shift : shiftRepository.findByRoster_IdOrderByStartAtAscIdAsc(roster.getId())) {
            validateShiftPeriod(roster, request.periodStart(), request.periodEnd(),
                    shift.getStartAt(), shift.getEndAt());
        }
        roster.updatePeriod(request.periodStart(), request.periodEnd());
        rosterRepository.flush();
        return response(roster);
    }

    @Transactional(readOnly = true)
    public List<RosterSummaryResponse> list(UUID prisonId, LocalDate from, LocalDate to, RosterStatus status) {
        validateDateRange(from, to);
        UUID effectivePrison = prisonAccess.effectivePrison(prisonId);
        return rosterRepository.search(effectivePrison, from, to, status)
                .stream().map(RosterSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RosterResponse get(UUID rosterId) {
        Roster roster = findRoster(rosterId);
        requirePrisonAccess(roster);
        return response(roster);
    }

    @Transactional
    public RosterShiftResponse addShift(UUID rosterId, CreateShiftRequest request) {
        Roster roster = findRosterForUpdate(rosterId);
        requireDraft(roster);
        User officer = lockOfficer(request.officerId());
        validateOfficer(officer, roster);
        validateShiftPeriod(roster, request.startAt(), request.endAt());
        rejectOverlap(officer, request.startAt(), request.endAt(), null);
        RosterShift shift = shiftRepository.saveAndFlush(new RosterShift(
                roster, officer, request.startAt(), request.endAt(), request.location()));
        return RosterShiftResponse.from(shift);
    }

    @Transactional
    public RosterShiftResponse updateShift(UUID rosterId, UUID shiftId, UpdateShiftRequest request) {
        Roster roster = findRosterForUpdate(rosterId);
        RosterShift shift = findShift(roster, shiftId);
        requireEditable(roster);

        Long oldOfficerId = shift.getAssignedOfficer().getId();
        Long requestedOfficerId = userRepository.findByPublicUuid(request.officerId())
                .map(User::getId).orElseThrow(OfficerNotFoundException::new);
        Map<Long, User> lockedOfficers = lockOfficers(List.of(oldOfficerId, requestedOfficerId));
        User newOfficer = lockedOfficers.get(requestedOfficerId);
        validateOfficer(newOfficer, roster);
        validateShiftPeriod(roster, request.startAt(), request.endAt());
        rejectOverlap(newOfficer, request.startAt(), request.endAt(), shift.getId());

        User oldOfficer = lockedOfficers.get(oldOfficerId);
        boolean published = roster.getStatus() == RosterStatus.PUBLISHED;
        OffsetDateTime oldStartAt = shift.getStartAt();
        OffsetDateTime oldEndAt = shift.getEndAt();
        String oldLocation = shift.getLocation();
        shift.update(newOfficer, request.startAt(), request.endAt(), request.location());
        shiftRepository.flush();
        if (published) {
            notifyShiftChanged(roster, shift, oldOfficer, oldStartAt, oldEndAt, oldLocation);
        }
        return RosterShiftResponse.from(shift);
    }

    @Transactional
    public void deleteShift(UUID rosterId, UUID shiftId) {
        Roster roster = findRosterForUpdate(rosterId);
        RosterShift shift = findShift(roster, shiftId);
        requireEditable(roster);
        User officer = userRepository.findByIdForUpdate(shift.getAssignedOfficer().getId())
                .orElseThrow(OfficerNotFoundException::new);
        if (roster.getStatus() == RosterStatus.PUBLISHED) {
            notifyOfficer(officer.getId(), "DUTY_SHIFT_CANCELLED", "Duty shift cancelled",
                    "Your shift was cancelled. " + shiftDetails(roster, shift), roster.getPublicUuid());
        }
        shiftRepository.delete(shift);
        shiftRepository.flush();
    }

    @Transactional
    public RosterResponse publish(UUID rosterId) {
        Roster roster = findRosterForUpdate(rosterId);
        requireDraft(roster);
        List<RosterShift> shifts = shiftRepository.findByRoster_IdOrderByStartAtAscIdAsc(roster.getId());
        if (shifts.isEmpty()) {
            throw new InvalidRosterStateException("A roster must have at least one shift before publication.");
        }

        Map<Long, User> lockedOfficers = lockOfficers(
                shifts.stream().map(shift -> shift.getAssignedOfficer().getId()).toList());
        for (RosterShift shift : shifts) {
            User officer = lockedOfficers.get(shift.getAssignedOfficer().getId());
            validateOfficer(officer, roster);
            validateShiftPeriod(roster, shift.getStartAt(), shift.getEndAt());
            rejectOverlap(officer, shift.getStartAt(), shift.getEndAt(), shift.getId());
        }

        roster.publish(Instant.now());
        rosterRepository.flush();
        notifyPublishedRoster(roster, shifts);
        return response(roster);
    }

    @Transactional
    public RosterResponse close(UUID rosterId) {
        Roster roster = findRosterForUpdate(rosterId);
        if (roster.getStatus() != RosterStatus.PUBLISHED) {
            throw new InvalidRosterStateException("Only a published roster can be closed.");
        }
        if (!LocalDate.now(rosterTimeZone).isAfter(roster.getPeriodEnd())) {
            throw new InvalidRosterStateException("A roster can be closed only after its period has ended.");
        }
        roster.close();
        rosterRepository.flush();
        return response(roster);
    }

    @Transactional(readOnly = true)
    public List<RosterShiftResponse> myShifts(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        User officer = prisonAccess.currentUser();
        OffsetDateTime fromInclusive = from.atStartOfDay(rosterTimeZone).toOffsetDateTime();
        OffsetDateTime toExclusive = to.plusDays(1).atStartOfDay(rosterTimeZone).toOffsetDateTime();
        return shiftRepository.findPublishedForOfficerBetween(
                        officer.getId(), fromInclusive, toExclusive)
                .stream().map(RosterShiftResponse::from).toList();
    }

    private Roster findRoster(UUID rosterId) {
        return rosterRepository.findByPublicUuid(rosterId).orElseThrow(RosterNotFoundException::new);
    }

    private Roster findRosterForUpdate(UUID rosterId) {
        Roster roster = rosterRepository.findByPublicUuidForUpdate(rosterId)
                .orElseThrow(RosterNotFoundException::new);
        requirePrisonAccess(roster);
        return roster;
    }

    private void requirePrisonAccess(Roster roster) {
        prisonAccess.requirePrison(roster.getPrison().getPublicUuid());
    }

    private RosterShift findShift(Roster roster, UUID shiftId) {
        return shiftRepository.findByRoster_IdAndPublicUuid(roster.getId(), shiftId)
                .orElseThrow(ShiftNotFoundException::new);
    }

    private User lockOfficer(UUID publicUuid) {
        return userRepository.findByPublicUuidForUpdate(publicUuid)
                .orElseThrow(OfficerNotFoundException::new);
    }

    private Map<Long, User> lockOfficers(Collection<Long> ids) {
        Map<Long, User> locked = new LinkedHashMap<>();
        ids.stream().distinct().sorted().forEach(id -> locked.put(id,
                userRepository.findByIdForUpdate(id).orElseThrow(OfficerNotFoundException::new)));
        return locked;
    }

    private void validateOfficer(User officer, Roster roster) {
        if (!officer.isActive() || !ELIGIBLE_OFFICER_ROLE.equals(officer.getRole())
                || officer.getPrison() == null
                || !officer.getPrison().getId().equals(roster.getPrison().getId())) {
            throw new InvalidOfficerAssignmentException();
        }
    }

    private void validateShiftPeriod(Roster roster, OffsetDateTime startAt, OffsetDateTime endAt) {
        validateShiftPeriod(roster, roster.getPeriodStart(), roster.getPeriodEnd(), startAt, endAt);
    }

    private void validateShiftPeriod(Roster roster, LocalDate periodStart, LocalDate periodEnd,
                                    OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)
                || Duration.between(startAt, endAt).compareTo(MAX_SHIFT_DURATION) > 0) {
            throw new InvalidShiftTimeException();
        }
        Instant periodStartInstant = periodStart.atStartOfDay(rosterTimeZone).toInstant();
        Instant periodEndExclusive = periodEnd.plusDays(1).atStartOfDay(rosterTimeZone).toInstant();
        if (startAt.toInstant().isBefore(periodStartInstant)
                || !startAt.toInstant().isBefore(periodEndExclusive)
                || endAt.toInstant().isAfter(periodEndExclusive)) {
            throw new ShiftOutsideRosterPeriodException();
        }
    }

    private void rejectOverlap(User officer, OffsetDateTime startAt, OffsetDateTime endAt, Long excludedShiftId) {
        if (shiftRepository.existsOverlappingShift(officer.getId(), startAt, endAt, excludedShiftId)) {
            throw new OverlappingShiftException();
        }
    }

    private void requireDraft(Roster roster) {
        if (roster.getStatus() != RosterStatus.DRAFT) {
            throw new InvalidRosterStateException("Only draft rosters can be changed or published.");
        }
    }

    private void requireEditable(Roster roster) {
        if (roster.getStatus() == RosterStatus.CLOSED) {
            throw new InvalidRosterStateException("Closed rosters are read-only.");
        }
    }

    private void notifyPublishedRoster(Roster roster, List<RosterShift> shifts) {
        Map<Long, List<RosterShift>> shiftsByOfficer = shifts.stream().collect(Collectors.groupingBy(
                shift -> shift.getAssignedOfficer().getId(), LinkedHashMap::new, Collectors.toList()));
        shiftsByOfficer.forEach((officerId, assignedShifts) -> {
            String details = assignedShifts.stream()
                    .map(shift -> shiftDetails(roster, shift))
                    .collect(Collectors.joining("; "));
            notifyOfficer(officerId, "DUTY_ROSTER_PUBLISHED", "Duty roster published",
                    "Roster " + roster.getPublicUuid() + " is published. " + details,
                    roster.getPublicUuid());
        });
    }

    private void notifyShiftChanged(Roster roster, RosterShift shift, User oldOfficer,
                                    OffsetDateTime oldStartAt, OffsetDateTime oldEndAt, String oldLocation) {
        User newOfficer = shift.getAssignedOfficer();
        if (oldOfficer.getId().equals(newOfficer.getId())) {
            notifyOfficer(newOfficer.getId(), "DUTY_SHIFT_UPDATED", "Duty shift updated",
                    "Your duty shift was updated. " + shiftDetails(roster, shift), roster.getPublicUuid());
            return;
        }
        String previousDetails = "Roster " + roster.getPublicUuid() + ", shift " + shift.getPublicUuid()
                + ", " + oldStartAt + " to " + oldEndAt + ", location " + oldLocation + ".";
        notifyOfficer(oldOfficer.getId(), "DUTY_SHIFT_UPDATED", "Duty shift reassigned",
                "You are no longer assigned to this duty shift. " + previousDetails,
                roster.getPublicUuid());
        notifyOfficer(newOfficer.getId(), "DUTY_SHIFT_ASSIGNED", "Duty shift assigned",
                "You have been assigned a duty shift. " + shiftDetails(roster, shift),
                roster.getPublicUuid());
    }

    private void notifyOfficer(Long officerId, String type, String title, String message, UUID rosterId) {
        notificationService.notifyUser(officerId, type, title, message, rosterId);
    }

    private String shiftDetails(Roster roster, RosterShift shift) {
        return "Roster " + roster.getPublicUuid() + ", shift " + shift.getPublicUuid()
                + ", " + shift.getStartAt() + " to " + shift.getEndAt()
                + ", location " + shift.getLocation() + ".";
    }

    private RosterResponse response(Roster roster) {
        List<RosterShift> shifts = shiftRepository.findByRoster_IdOrderByStartAtAscIdAsc(roster.getId());
        return RosterResponse.from(roster, shifts);
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must be on or after the start date.");
        }
    }

    public static class RosterNotFoundException extends RuntimeException { }
    public static class ShiftNotFoundException extends RuntimeException { }
    public static class PrisonNotFoundException extends RuntimeException { }
    public static class InactivePrisonException extends RuntimeException { }
    public static class OfficerNotFoundException extends RuntimeException { }
    public static class InvalidOfficerAssignmentException extends RuntimeException { }
    public static class InvalidShiftTimeException extends RuntimeException { }
    public static class ShiftOutsideRosterPeriodException extends RuntimeException { }
    public static class OverlappingShiftException extends RuntimeException { }

    public static class InvalidRosterStateException extends RuntimeException {
        public InvalidRosterStateException(String message) {
            super(message);
        }
    }
}
