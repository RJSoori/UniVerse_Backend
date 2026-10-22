package com.example.backend_service.skills;

import com.example.backend_service.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CV upload must be gated on the student explicitly accepting the notice that their CV is sent
 * to Google's Gemini API - see StudentSkillProfile#cvPrivacyPolicyAcceptedAt. The gate is
 * enforced server-side (not just hidden client-side) and is one-time per student.
 */
public class SkillsControllerTest {

    private StudentSkillProfileRepository repository;
    private com.example.backend_service.AzureBlobService azureBlobService;
    private GeminiSkillExtractionService extractionService;
    private SkillsController controller;
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        repository = Mockito.mock(StudentSkillProfileRepository.class);
        azureBlobService = Mockito.mock(com.example.backend_service.AzureBlobService.class);
        extractionService = Mockito.mock(GeminiSkillExtractionService.class);
        controller = new SkillsController(repository, azureBlobService, extractionService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        authenticateAsStudent(5L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsStudent(long studentId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        studentId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    private StudentSkillProfile profileFor(Long studentId, Instant policyAcceptedAt) {
        StudentSkillProfile profile = new StudentSkillProfile();
        profile.setId(1L);
        profile.setStudentId(studentId);
        profile.setSkillsJson("[]");
        profile.setCvPrivacyPolicyAcceptedAt(policyAcceptedAt);
        return profile;
    }

    @Test
    void uploadCv_beforePolicyAccepted_returns403WithoutTouchingExtractionOrBlobStorage() throws Exception {
        when(repository.findByStudentId(5L)).thenReturn(Optional.of(profileFor(5L, null)));
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/api/skills/cv").file(file))
                .andExpect(status().isForbidden());

        verifyNoInteractions(extractionService);
        verifyNoInteractions(azureBlobService);
    }

    @Test
    void uploadCv_afterPolicyAccepted_passesTheGate() throws Exception {
        when(repository.findByStudentId(5L)).thenReturn(Optional.of(profileFor(5L, Instant.now())));
        // Empty file - fails validation *after* the policy gate, proving the gate let it through
        // rather than that this file happened to be acceptable.
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/skills/cv").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptCvPrivacyPolicy_setsAcceptedFlagAndIsReflectedInResponse() throws Exception {
        StudentSkillProfile profile = profileFor(5L, null);
        when(repository.findByStudentId(5L)).thenReturn(Optional.of(profile));
        when(repository.save(any(StudentSkillProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        String body = mockMvc.perform(post("/api/skills/cv-privacy-policy/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvPrivacyPolicyAccepted").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(profile.getCvPrivacyPolicyAcceptedAt()).isNotNull();
        Map<?, ?> json = mapper.readValue(body, Map.class);
        assertThat(json.get("cvPrivacyPolicyAccepted")).isEqualTo(true);
    }

    @Test
    void getSkills_reflectsAcceptanceFlagBothWays() throws Exception {
        when(repository.findByStudentId(5L)).thenReturn(Optional.of(profileFor(5L, null)));
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvPrivacyPolicyAccepted").value(false));

        when(repository.findByStudentId(5L)).thenReturn(Optional.of(profileFor(5L, Instant.now())));
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvPrivacyPolicyAccepted").value(true));
    }
}
