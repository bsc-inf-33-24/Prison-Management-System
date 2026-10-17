package com.pms.api.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pms.api.cases.dto.CaseRequest;
import com.pms.api.cases.dto.CaseResponse;
import com.pms.api.cases.dto.CaseStatusRequest;
import com.pms.api.cases.entity.CaseStatus;
import com.pms.api.cases.repository.CaseRepository;
import com.pms.api.cases.repository.CrimeRepository;
import com.pms.api.cases.service.CaseService;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.sentence.dto.SentenceRequest;
import com.pms.api.sentence.dto.SentenceResponse;
import com.pms.api.sentence.service.SentenceService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(com.pms.api.TestcontainersConfiguration.class)
class CaseSentenceManagementIntegrationTest {

    @Autowired
    private CaseService caseService;

    @Autowired
    private SentenceService sentenceService;

    @Autowired
    private InmateRepository inmateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CrimeRepository crimeRepository;

    @Autowired
    private CaseRepository caseRepository;

    private User caseOfficer;

    @BeforeEach
    void setUp() {
        caseOfficer = userRepository.saveAndFlush(
                new User("Case Test", "case-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "unused-password-hash", "Case Officer", Set.of()));
    }

    @Test
    void createMatchingCaseAndSentence_returnsConsistentIds() {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Alice", "Jones", LocalDate.of(1995, 1, 10),
                "Female", LocalDate.of(2024, 1, 1), "Minimum", "Main Facility"));
        var crime = crimeRepository.saveAndFlush(new com.pms.api.cases.entity.Crime(
                "Theft", "Property theft", "Property"));

        CaseResponse createdCase = caseService.createCase(
                new CaseRequest(inmate.getPublicUuid(), "CASE-1001", "County Court",
                        LocalDate.of(2025, 1, 3), LocalDate.of(2025, 1, 17), Set.of(crime.getPublicUuid())),
                caseOfficer.getUsername());

        SentenceResponse createdSentence = sentenceService.create(new SentenceRequest(
                createdCase.id(), inmate.getPublicUuid(), 12, LocalDate.of(2025, 1, 5),
                LocalDate.of(2025, 1, 16), "active"));

        assertThat(createdCase.id()).isNotNull();
        assertThat(createdCase.inmate().id()).isEqualTo(inmate.getPublicUuid());
        assertThat(createdCase.crimes()).hasSize(1);
        assertThat(createdSentence.caseId()).isEqualTo(createdCase.id());
        assertThat(createdSentence.inmateId()).isEqualTo(inmate.getPublicUuid());
        assertThat(createdSentence.status()).isEqualTo("active");
        assertThat(caseRepository.findByPublicUuid(createdCase.id()).orElseThrow().getStatus()).isEqualTo(CaseStatus.ONGOING);
    }

    @Test
    void sentenceServiceRejectsMismatchedCaseAndInmateBeforePersisting() {
        Inmate inmateOne = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Alicia", "Smith", LocalDate.of(1990, 5, 5),
                "Female", LocalDate.of(2023, 6, 1), "Minimum", "Main Facility"));
        Inmate inmateTwo = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Bobby", "Brown", LocalDate.of(1991, 7, 6),
                "Male", LocalDate.of(2023, 7, 1), "Medium", "Main Facility"));
        var crime = crimeRepository.saveAndFlush(new com.pms.api.cases.entity.Crime(
                "Fraud", "Credit fraud", "Financial"));

        CaseResponse createdCase = caseService.createCase(
                new CaseRequest(inmateOne.getPublicUuid(), "CASE-2002", "District Court",
                        LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 15), Set.of(crime.getPublicUuid())),
                caseOfficer.getUsername());

        assertThrows(SentenceService.InvalidSentenceRequestException.class, () -> sentenceService.create(new SentenceRequest(
                createdCase.id(), inmateTwo.getPublicUuid(), 6,
                LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 9), "active")));
    }

    @Test
    void sentenceServiceRejectsClosedCasesAndReversedDates() {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Carla", "Davis", LocalDate.of(1988, 8, 8),
                "Female", LocalDate.of(2024, 8, 1), "Minimum", "Main Facility"));
        var crime = crimeRepository.saveAndFlush(new com.pms.api.cases.entity.Crime(
                "Assault", "Simple assault", "Violent"));

        CaseResponse createdCase = caseService.createCase(
                new CaseRequest(inmate.getPublicUuid(), "CASE-3003", "High Court",
                        LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 20), Set.of(crime.getPublicUuid())),
                caseOfficer.getUsername());

        caseService.updateStatus(createdCase.id(), new CaseStatusRequest("closed", "Conviction confirmed"));
        assertThrows(SentenceService.InvalidSentenceRequestException.class, () -> sentenceService.create(new SentenceRequest(
                createdCase.id(), inmate.getPublicUuid(), 12,
                LocalDate.of(2025, 3, 11), LocalDate.of(2025, 3, 10), "completed")));

        assertThrows(SentenceService.InvalidSentenceRequestException.class, () -> sentenceService.create(new SentenceRequest(
                createdCase.id(), inmate.getPublicUuid(), 12,
                LocalDate.of(2025, 3, 11), LocalDate.of(2025, 3, 10), "completed")));
    }

    @Test
    void caseStatusTransitionRequiresReasonForClosedAndAppealed() {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Darren", "Mills", LocalDate.of(1992, 9, 10),
                "Male", LocalDate.of(2024, 9, 1), "Medium", "Main Facility"));
        var crime = crimeRepository.saveAndFlush(new com.pms.api.cases.entity.Crime(
                "Drug offence", "Possession", "Drug"));

        CaseResponse createdCase = caseService.createCase(
                new CaseRequest(inmate.getPublicUuid(), "CASE-4004", "Appeal Court",
                        LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30), Set.of(crime.getPublicUuid())),
                caseOfficer.getUsername());

        assertThrows(CaseService.InvalidCaseRequestException.class,
                () -> caseService.updateStatus(createdCase.id(), new CaseStatusRequest("closed", "")));
        assertThrows(CaseService.InvalidCaseRequestException.class,
                () -> caseService.updateStatus(createdCase.id(), new CaseStatusRequest("appealed", null)));
    }

    private String uniqueInmateNumber() {
        return "CASE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
