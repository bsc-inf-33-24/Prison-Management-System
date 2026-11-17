package com.pms.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.entity.InmateType;
import com.pms.api.inmate.entity.SentenceType;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.notification.entity.Notification;
import com.pms.api.notification.repository.NotificationRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.sentence.entity.InmateSentence;
import com.pms.api.sentence.repository.InmateSentenceRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
        "app.storage.minio.endpoint=http://localhost:9000",
        "app.storage.minio.access-key=test-access-key",
        "app.storage.minio.secret-key=test-secret-key",
        "app.storage.minio.bucket=test-inmate-photos",
        "app.storage.minio.max-photo-bytes=5242880"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SentenceExtensionApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PrisonRepository prisons;
    @Autowired private InmateRepository inmates;
    @Autowired private InmateSentenceRepository sentences;
    @Autowired private JwtService jwtService;
    @Autowired private NotificationRepository notifications;

    private Prison prison;
    private String requesterToken;
    private String reviewerToken;
    private String commissionerToken;
    private User commissioner;
    private String otherPrisonReviewerToken;

    @BeforeEach
    void setUp() {
        prison = prisons.saveAndFlush(new Prison("E" + shortId(), "Extension Test Prison", null));
        Prison otherPrison = prisons.saveAndFlush(new Prison("E" + shortId(), "Other Test Prison", null));
        requesterToken = tokenFor("Records Officer", "Requesting Officer", prison);
        reviewerToken = tokenFor("Prison Officer", "Reviewing Officer", prison);
        commissioner = users.saveAndFlush(new User("Commissioner Reviewer", "e" + shortId(),
                "unused-password-hash", "Commissioner", Set.of()));
        commissionerToken = jwtService.generateToken(commissioner);
        otherPrisonReviewerToken = tokenFor("Prison Officer", "Other Prison Officer", otherPrison);
    }

    @Test
    void requestRemainsPendingUntilDifferentOfficerApprovesAndApprovalUpdatesSentence() throws Exception {
        InmateSentence sentence = createSentence();
        LocalDate originalEndDate = sentence.getEndDate();
        String requestId = mockMvc.perform(post("/api/v1/sentences/" + sentence.getPublicUuid() + "/extensions")
                        .header("Authorization", bearer(requesterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-01", "Court order 24/2029")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalEndDate").value(originalEndDate.toString()))
                .andExpect(jsonPath("$.proposedEndDate").value("2030-01-01"))
                .andExpect(jsonPath("$.reason").value("Court order 24/2029"))
                .andExpect(jsonPath("$.requester").value("Requesting Officer"))
                .andExpect(jsonPath("$.reviewer").value(nullValue()))
                .andExpect(jsonPath("$.decision").value("PENDING"))
                .andExpect(jsonPath("$.requestedAt").isNotEmpty())
                .andExpect(jsonPath("$.decidedAt").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        String extensionId = objectMapper.readTree(requestId).path("id").asText();

        assertThat(notifications.findByRecipientUserIdOrderByCreatedAtDesc(commissioner.getId()))
                .extracting(Notification::getType)
                .contains("SENTENCE_EXTENSION_REQUESTED");
        assertThat(notifications.findByRecipientUserIdOrderByCreatedAtDesc(commissioner.getId()))
                .extracting(Notification::getRelatedRecordId)
                .contains(extensionId);

        assertEquals(originalEndDate, sentences.findByPublicUuid(sentence.getPublicUuid()).orElseThrow().getEndDate());
        decide(extensionId, requesterToken, "APPROVED", 409);

        mockMvc.perform(post("/api/v1/sentence-extensions/" + extensionId + "/decision")
                        .header("Authorization", bearer(commissionerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody("APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.reviewer").value("Commissioner Reviewer"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());

        assertEquals(LocalDate.of(2030, 1, 1),
                sentences.findByPublicUuid(sentence.getPublicUuid()).orElseThrow().getEndDate());
        decide(extensionId, requesterToken, "REJECTED", 409);
    }

    @Test
    void rejectionAndDuplicatePendingRequestsDoNotChangeSentenceEndDate() throws Exception {
        InmateSentence sentence = createSentence();
        LocalDate originalEndDate = sentence.getEndDate();
        String path = "/api/v1/sentences/" + sentence.getPublicUuid() + "/extensions";

        String response = mockMvc.perform(post(path)
                        .header("Authorization", bearer(requesterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2029-01-01", "Approved by sentencing authority")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String extensionId = objectMapper.readTree(response).path("id").asText();

        mockMvc.perform(post(path)
                        .header("Authorization", bearer(requesterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-01", "Duplicate pending request")))
                .andExpect(status().isConflict());
        assertEquals(originalEndDate, sentences.findByPublicUuid(sentence.getPublicUuid()).orElseThrow().getEndDate());

        decide(extensionId, commissionerToken, "REJECTED", 200);
        assertEquals(originalEndDate, sentences.findByPublicUuid(sentence.getPublicUuid()).orElseThrow().getEndDate());
        mockMvc.perform(get(path).header("Authorization", bearer(requesterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decision").value("REJECTED"))
                .andExpect(jsonPath("$[0].reviewer").value("Commissioner Reviewer"))
                .andExpect(jsonPath("$[0].originalEndDate").value(originalEndDate.toString()));
    }

    @Test
    void invalidDatesAndCrossPrisonReviewAreRejected() throws Exception {
        InmateSentence sentence = createSentence();
        String path = "/api/v1/sentences/" + sentence.getPublicUuid() + "/extensions";
        mockMvc.perform(post(path)
                        .header("Authorization", bearer(requesterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2027-01-01", "Not later than current end date")))
                .andExpect(status().isBadRequest());

        String response = mockMvc.perform(post(path)
                        .header("Authorization", bearer(requesterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2029-01-01", "Valid proposal")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String extensionId = objectMapper.readTree(response).path("id").asText();
        decide(extensionId, reviewerToken, "APPROVED", 403);
        decide(extensionId, otherPrisonReviewerToken, "APPROVED", 403);
    }

    private InmateSentence createSentence() {
        Inmate inmate = new Inmate("Taylor", "Example", LocalDate.of(1990, 1, 1), "Other",
                LocalDate.of(2020, 1, 1), InmateType.SENTENCED, "Test inmate", "Not recorded",
                null, null, null, null, null, null);
        inmate.assignPrison(prison);
        inmate = inmates.saveAndFlush(inmate);
        return sentences.saveAndFlush(new InmateSentence(inmate, "Test offence", SentenceType.FIXED_TERM,
                LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1)));
    }

    private String tokenFor(String role, String fullName, Prison assignedPrison) {
        User user = new User(fullName, "e" + shortId(), "unused-password-hash", role, Set.of());
        if (assignedPrison != null) user.assignPrison(assignedPrison);
        return jwtService.generateToken(users.saveAndFlush(user));
    }

    private void decide(String extensionId, String token, String decision, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/sentence-extensions/" + extensionId + "/decision")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody(decision)))
                .andExpect(status().is(expectedStatus));
    }

    private String requestBody(String proposedEndDate, String reason) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("proposedEndDate", proposedEndDate);
        body.put("reason", reason);
        return objectMapper.writeValueAsString(body);
    }

    private String decisionBody(String decision) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("decision", decision);
        return objectMapper.writeValueAsString(body);
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private static String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
}
