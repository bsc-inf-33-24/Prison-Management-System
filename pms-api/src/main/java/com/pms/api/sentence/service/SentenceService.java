package com.pms.api.sentence.service;

import com.pms.api.cases.entity.Case;
import com.pms.api.cases.repository.CaseRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.sentence.dto.SentenceRequest;
import com.pms.api.sentence.dto.SentenceResponse;
import com.pms.api.sentence.entity.Sentence;
import com.pms.api.sentence.repository.SentenceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SentenceService {

    private final SentenceRepository sentenceRepository;
    private final InmateRepository inmateRepository;
    private final CaseRepository caseRepository;

    public SentenceService(SentenceRepository sentenceRepository,
                          InmateRepository inmateRepository,
                          CaseRepository caseRepository) {
        this.sentenceRepository = sentenceRepository;
        this.inmateRepository = inmateRepository;
        this.caseRepository = caseRepository;
    }

    @Audited
    @Transactional
    public SentenceResponse create(SentenceRequest request) {
        Case caseEntity = findCase(request.caseId());
        Inmate inmate = findInmate(request.inmateId());
        Sentence sentence = Sentence.create(
                caseEntity,
                inmate,
                request.duration(),
                request.startDate(),
                request.releaseDate(),
                request.status()
        );
        return SentenceResponse.from(sentenceRepository.saveAndFlush(sentence));
    }

    @Transactional(readOnly = true)
    public List<SentenceResponse> list(Long inmateId, Long caseId) {
        List<Sentence> sentences;
        if (inmateId != null) {
            sentences = sentenceRepository.findByInmateIdOrderByIdAsc(inmateId);
        } else if (caseId != null) {
            sentences = sentenceRepository.findByCaseEntityIdOrderByIdAsc(caseId);
        } else {
            sentences = sentenceRepository.findAllByOrderByIdAsc();
        }
        return sentences.stream().map(SentenceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SentenceResponse get(Long sentenceId) {
        return SentenceResponse.from(findSentence(sentenceId));
    }

    @Audited
    @Transactional
    public SentenceResponse update(Long sentenceId, SentenceRequest request) {
        Sentence sentence = findSentence(sentenceId);
        Case caseEntity = findCase(request.caseId());
        Inmate inmate = findInmate(request.inmateId());
        sentence.updateDetails(
                caseEntity,
                inmate,
                request.duration(),
                request.startDate(),
                request.releaseDate(),
                request.status()
        );
        return SentenceResponse.from(sentenceRepository.saveAndFlush(sentence));
    }

    private Case findCase(Long caseId) {
        return caseRepository.findById(caseId).orElseThrow(CaseNotFoundException::new);
    }

    private Inmate findInmate(Long inmateId) {
        return inmateRepository.findById(inmateId).orElseThrow(InmateNotFoundException::new);
    }

    private Sentence findSentence(Long sentenceId) {
        return sentenceRepository.findById(sentenceId).orElseThrow(SentenceNotFoundException::new);
    }

    public static class SentenceNotFoundException extends RuntimeException {
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class CaseNotFoundException extends RuntimeException {
    }
}
