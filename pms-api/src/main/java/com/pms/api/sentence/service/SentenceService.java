package com.pms.api.sentence.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.sentence.dto.*;
import com.pms.api.sentence.entity.*;
import com.pms.api.sentence.repository.*;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
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
    private final UserRepository users;
    private final PrisonAccessService prisonAccess;
    public SentenceService(InmateRepository inmates, InmateSentenceRepository sentences,
            SentenceRemissionRepository remissions, UserRepository users, PrisonAccessService prisonAccess) {
        this.inmates = inmates; this.sentences = sentences; this.remissions = remissions;
        this.users = users; this.prisonAccess = prisonAccess;
    }
    @Transactional
    public SentenceResponse create(CreateSentenceRequest request) {
        Inmate inmate = inmate(request.inmateId());
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        return SentenceResponse.from(sentences.save(new InmateSentence(inmate, request.offence().trim(),
                request.sentenceType(), request.startDate(), request.endDate())));
    }
    @Transactional(readOnly = true)
    public List<SentenceResponse> list(UUID inmateId) {
        Inmate inmate = inmate(inmateId);
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        return sentences.findByInmateIdOrderByStartDateDescIdDesc(inmate.getId()).stream()
                .map(SentenceResponse::from).toList();
    }
    @Transactional
    public SentenceResponse updateStatus(UUID sentenceId, com.pms.api.sentence.entity.SentenceStatus status) {
        InmateSentence sentence = sentences.findByPublicUuid(sentenceId).orElseThrow(SentenceNotFoundException::new);
        Inmate inmate = sentence.getInmate();
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        sentence.changeStatus(status);
        return SentenceResponse.from(sentence);
    }
    @Transactional
    public RemissionResponse addRemission(UUID sentenceId, CreateRemissionRequest request, String username) {
        InmateSentence sentence = sentences.findByPublicUuid(sentenceId).orElseThrow(SentenceNotFoundException::new);
        Inmate inmate = sentence.getInmate();
        prisonAccess.requirePrison(inmate.getCurrentPrison() == null ? null : inmate.getCurrentPrison().getPublicUuid());
        if (sentence.getSentenceType() != com.pms.api.inmate.entity.SentenceType.FIXED_TERM)
            throw new IllegalArgumentException("Remission can only be applied to a fixed-term sentence.");
        User approver = users.findByUsername(username).orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
        BigDecimal totalPercentage = remissions.sumPercentageForSentence(sentence.getId())
                .add(request.remissionPercentage());
        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Total remission for a sentence cannot exceed 100 percent.");
        SentenceRemission saved = remissions.save(new SentenceRemission(sentence,
                request.remissionPercentage(), request.effectiveDate(), request.reason().trim(), approver));
        return RemissionResponse.from(saved, projectedReleaseDate(sentence, totalPercentage));
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
    private static java.time.LocalDate projectedReleaseDate(InmateSentence sentence, BigDecimal remissionPercentage) {
        if (sentence.getSentenceType() != com.pms.api.inmate.entity.SentenceType.FIXED_TERM) return null;
        long originalDays = ChronoUnit.DAYS.between(sentence.getStartDate(), sentence.getEndDate()) + 1;
        long remainingDays = BigDecimal.valueOf(originalDays)
                .multiply(BigDecimal.ONE.subtract(remissionPercentage.movePointLeft(2)))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        return sentence.getStartDate().plusDays(Math.max(0, remainingDays - 1));
    }
    private Inmate inmate(UUID id) { return inmates.findByPublicUuid(id).orElseThrow(InmateNotFoundException::new); }
    public static class InmateNotFoundException extends RuntimeException { }
    public static class SentenceNotFoundException extends RuntimeException { }
}
