package com.pms.api.sentence.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.sentence.dto.*;
import com.pms.api.sentence.entity.*;
import com.pms.api.sentence.repository.*;
import com.pms.api.user.entity.User;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SentenceService {
    private final InmateRepository inmates;
    private final InmateSentenceRepository sentences;
    private final SentenceRemissionRepository remissions;
    private final PrisonAccessService prisonAccess;
    public SentenceService(InmateRepository inmates, InmateSentenceRepository sentences,
            SentenceRemissionRepository remissions, PrisonAccessService prisonAccess) {
        this.inmates = inmates; this.sentences = sentences; this.remissions = remissions;
        this.prisonAccess = prisonAccess;
    }
    @Transactional
    public SentenceResponse create(CreateSentenceRequest request) {
        Inmate inmate = inmate(request.inmateId());
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        InmateSentence sentence = sentences.save(new InmateSentence(inmate, request.offence().trim(),
                request.sentenceType(), request.startDate(), request.endDate()));
        if (request.remission() != null) {
            requireFixedTerm(sentence);
            User approver = currentUser();
            remissions.save(new SentenceRemission(sentence, request.remission().remissionPercentage(),
                    request.remission().effectiveDate(), request.remission().reason().trim(), approver));
        }
        return sentenceResponse(sentence);
    }
    @Transactional(readOnly = true)
    public List<SentenceResponse> list(UUID inmateId) {
        Inmate inmate = inmate(inmateId);
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        return sentences.findByInmateIdOrderByStartDateDescIdDesc(inmate.getId()).stream()
                .map(this::sentenceResponse).toList();
    }
    @Transactional(readOnly = true)
    public SentenceResponse get(UUID sentenceId) {
        InmateSentence sentence = sentences.findByPublicUuid(sentenceId).orElseThrow(SentenceNotFoundException::new);
        Inmate inmate = sentence.getInmate();
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        return sentenceResponse(sentence);
    }

    @Transactional
    public SentenceResponse update(UUID sentenceId, PatchSentenceRequest request) {
        InmateSentence sentence = sentences.findByPublicUuidForUpdate(sentenceId)
                .orElseThrow(SentenceNotFoundException::new);
        Inmate inmate = sentence.getInmate();
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        if (request.remission() != null) {
            requireFixedTerm(sentence);
            List<SentenceRemission> existing = remissions.findBySentenceIdOrderByEffectiveDateAscIdAsc(sentence.getId());
            SentenceRemission remission = existing.isEmpty() ? null : existing.get(0);
            BigDecimal percentage = request.remission().remissionPercentage() == null
                    ? remission == null ? BigDecimal.ZERO : remission.getRemissionPercentage()
                    : request.remission().remissionPercentage();
            if (request.remission().effectiveDate() == null && remission == null
                    || request.remission().reason() == null && remission == null)
                throw new IllegalArgumentException("A new remission requires effectiveDate and reason.");
            BigDecimal total = remissions.sumPercentageForSentence(sentence.getId());
            if (remission != null) total = total.subtract(remission.getRemissionPercentage());
            total = total.add(percentage);
            if (total.compareTo(BigDecimal.valueOf(100)) > 0)
                throw new IllegalArgumentException("Total remission for a sentence cannot exceed 100 percent.");
            if (remission == null) {
                remission = new SentenceRemission(sentence, percentage, request.remission().effectiveDate(),
                        normalizedReason(request.remission().reason()), currentUser());
                remissions.save(remission);
            } else {
                remission.update(percentage,
                        request.remission().effectiveDate() == null ? remission.getEffectiveDate() : request.remission().effectiveDate(),
                        request.remission().reason() == null ? remission.getReason() : normalizedReason(request.remission().reason()));
            }
        }
        return sentenceResponse(sentence);
    }
    @Transactional(readOnly = true)
    public List<RemissionResponse> listRemissions(UUID sentenceId) {
        InmateSentence sentence = sentences.findByPublicUuid(sentenceId).orElseThrow(SentenceNotFoundException::new);
        Inmate inmate = sentence.getInmate();
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        BigDecimal cumulativePercentage = BigDecimal.ZERO;
        java.util.ArrayList<RemissionResponse> responses = new java.util.ArrayList<>();
        for (SentenceRemission remission : remissions.findBySentenceIdOrderByEffectiveDateAscIdAsc(sentence.getId())) {
            cumulativePercentage = cumulativePercentage.add(remission.getRemissionPercentage());
            responses.add(RemissionResponse.from(remission, projectedReleaseDate(sentence, cumulativePercentage)));
        }
        return List.copyOf(responses);
    }

    private SentenceResponse sentenceResponse(InmateSentence sentence) {
        BigDecimal total = remissions.sumPercentageForSentence(sentence.getId());
        return SentenceResponse.from(sentence, remissions.findBySentenceIdOrderByEffectiveDateAscIdAsc(sentence.getId()),
                projectedReleaseDate(sentence, total));
    }

    private User currentUser() {
        return prisonAccess.currentUser();
    }

    private static void requireFixedTerm(InmateSentence sentence) {
        if (sentence.getSentenceType() != com.pms.api.inmate.entity.SentenceType.FIXED_TERM)
            throw new IllegalArgumentException("Remission can only be applied to a fixed-term sentence.");
    }

    private static java.time.LocalDate projectedReleaseDate(InmateSentence sentence, BigDecimal remissionPercentage) {
        if (sentence.getSentenceType() != com.pms.api.inmate.entity.SentenceType.FIXED_TERM) return null;
        long originalDays = ChronoUnit.DAYS.between(sentence.getStartDate(), sentence.getEndDate()) + 1;
        long remainingDays = BigDecimal.valueOf(originalDays)
                .multiply(BigDecimal.ONE.subtract(remissionPercentage.movePointLeft(2)))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        return sentence.getStartDate().plusDays(Math.max(0, remainingDays - 1));
    }
    private static String normalizedReason(String reason) {
        if (reason.isBlank()) throw new IllegalArgumentException("Remission reason must not be blank.");
        return reason.trim();
    }
    private Inmate inmate(UUID id) { return inmates.findByPublicUuid(id).orElseThrow(InmateNotFoundException::new); }
    public static class InmateNotFoundException extends RuntimeException { }
    public static class SentenceNotFoundException extends RuntimeException { }
}
