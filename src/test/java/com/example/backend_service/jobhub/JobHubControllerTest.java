package com.example.backend_service.jobhub;

import com.example.backend_service.RecruiterStatus;
import com.example.backend_service.common.exception.GlobalExceptionHandler;
import com.example.backend_service.jobhub.controller.JobHubController;
import com.example.backend_service.jobhub.dto.JobRequest;
import com.example.backend_service.jobhub.dto.JobReportRequest;
import com.example.backend_service.jobhub.dto.JobUpdateRequest;
import com.example.backend_service.jobhub.dto.RecruiterProfileResponse;
import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.model.JobReport;
import com.example.backend_service.jobhub.model.Recruiter;
import com.example.backend_service.jobhub.repository.JobReportRepository;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.repository.RecruiterRepository;
import com.example.backend_service.jobhub.service.RecruiterJwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JobHubControllerTest {

    private JobRepository jobRepository;
    private JobReportRepository jobReportRepository;
    private RecruiterRepository recruiterRepository;
    private JobHubController controller;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        jobRepository = Mockito.mock(JobRepository.class);
        jobReportRepository = Mockito.mock(JobReportRepository.class);
        recruiterRepository = Mockito.mock(RecruiterRepository.class);
        controller = new JobHubController();
        // inject mocks via reflection since controller uses field injection
        com.example.backend_service.TestUtils.setField(controller, "jobRepository", jobRepository);
        com.example.backend_service.TestUtils.setField(controller, "jobReportRepository", jobReportRepository);
        com.example.backend_service.TestUtils.setField(controller, "recruiterRepository", recruiterRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                // @AuthenticationPrincipal isn't resolved by default outside a real security
                // filter chain - the report endpoint needs it to read the reporting student's id.
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
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

    @Test
    void postJob_withVerifiedRecruiter_returnsOk() throws Exception {
        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setStatus(com.example.backend_service.RecruiterStatus.VERIFIED);
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(10L);
            return j;
        });

        JobRequest req = new JobRequest("Dev","desc","req","skills","0","remote","fulltime","now",5L);

        mockMvc.perform(post("/api/jobs/post")
                .contentType("application/json")
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllJobs_callsRepository() throws Exception {
        when(jobRepository.findByStatusAndActiveOrderByCreatedAtDescIdDesc(JobStatus.APPROVED, true))
                .thenReturn(List.of(new Job()));
        mockMvc.perform(get("/api/jobs/all")).andExpect(status().isOk());
    }

    @Test
    void getAllJobs_returnsNewestFirst() throws Exception {
        Job older = new Job();
        older.setId(1L);
        older.setTitle("Older Posting");
        older.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        Job newer = new Job();
        newer.setId(2L);
        newer.setTitle("Newer Posting");
        newer.setCreatedAt(Instant.now());
        // Repository is trusted to already return newest-first (ORDER BY createdAt DESC) -
        // this just confirms the controller passes that order straight through untouched.
        when(jobRepository.findByStatusAndActiveOrderByCreatedAtDescIdDesc(JobStatus.APPROVED, true))
                .thenReturn(List.of(newer, older));

        mockMvc.perform(get("/api/jobs/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Newer Posting"))
                .andExpect(jsonPath("$[1].title").value("Older Posting"));
    }

    @Test
    void setJobActive_ownedJob_updatesActiveFlag() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(40L);

        Job job = new Job();
        job.setId(7L);
        job.setActive(true);
        when(jobRepository.findByIdAndRecruiterId(7L, 40L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/jobs/recruiters/40/jobs/7/active")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("active", "false"))
                .andExpect(status().isOk());

        assertThat(job.isActive()).isFalse();
    }

    @Test
    void setJobActive_notOwnedByToken_returnsError() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(99L);
        when(jobRepository.findByIdAndRecruiterId(7L, 99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/jobs/recruiters/40/jobs/7/active")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("active", "false"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateJob_ownedJob_updatesAllFields() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(40L);

        Job job = new Job();
        job.setId(7L);
        job.setTitle("Old Title");
        when(jobRepository.findByIdAndRecruiterId(7L, 40L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobUpdateRequest update = new JobUpdateRequest(
                "New Title", "new desc", "new req", "Java, SQL", "2000", "remote", "full-time");

        mockMvc.perform(put("/api/jobs/recruiters/40/jobs/7")
                        .header("X-Recruiter-Token", "valid-token")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        assertThat(job.getTitle()).isEqualTo("New Title");
        assertThat(job.getDescription()).isEqualTo("new desc");
        assertThat(job.getSkills()).isEqualTo("Java, SQL");
        assertThat(job.getWorkType()).isEqualTo("remote");
    }

    @Test
    void deleteRecruiterJob_ownedJob_softDeletesInsteadOfRemoving() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(40L);

        Job job = new Job();
        job.setId(7L);
        job.setActive(true);
        job.setDeleted(false);
        when(jobRepository.findByIdAndRecruiterId(7L, 40L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(delete("/api/jobs/recruiters/40/jobs/7")
                        .header("X-Recruiter-Token", "valid-token"))
                .andExpect(status().isNoContent());

        assertThat(job.isDeleted()).isTrue();
        assertThat(job.isActive()).isFalse();
        verify(jobRepository, never()).delete(any(Job.class));
        verify(jobRepository).save(job);
    }

    // ---- postJob hard-blocks anything other than VERIFIED ----

    private void stubRecruiterForPostJob(RecruiterStatus status) {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setStatus(status);
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));
    }

    @Test
    void postJob_pendingRecruiter_returns403() throws Exception {
        stubRecruiterForPostJob(RecruiterStatus.PENDING);
        JobRequest req = new JobRequest("Dev", "desc", "req", "skills", "0", "remote", "fulltime", "now", 5L);

        mockMvc.perform(post("/api/jobs/post")
                        .header("X-Recruiter-Token", "valid-token")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postJob_reVerificationRecruiter_returns403() throws Exception {
        stubRecruiterForPostJob(RecruiterStatus.RE_VERIFICATION);
        JobRequest req = new JobRequest("Dev", "desc", "req", "skills", "0", "remote", "fulltime", "now", 5L);

        mockMvc.perform(post("/api/jobs/post")
                        .header("X-Recruiter-Token", "valid-token")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postJob_rejectedRecruiter_returns403() throws Exception {
        stubRecruiterForPostJob(RecruiterStatus.REJECTED);
        JobRequest req = new JobRequest("Dev", "desc", "req", "skills", "0", "remote", "fulltime", "now", 5L);

        mockMvc.perform(post("/api/jobs/post")
                        .header("X-Recruiter-Token", "valid-token")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ---- loginRecruiter allows RE_VERIFICATION, blocks PENDING/REJECTED ----

    private Recruiter stubLoginRecruiter(RecruiterStatus status, PasswordEncoder passwordEncoder) {
        Recruiter r = new Recruiter();
        r.setId(9L);
        r.setEmail("recruiter@test.com");
        r.setPassword("hashed");
        r.setStatus(status);
        when(recruiterRepository.findByEmail("recruiter@test.com")).thenReturn(r);
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        return r;
    }

    @Test
    void loginRecruiter_reVerificationStatus_issuesToken() throws Exception {
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        com.example.backend_service.TestUtils.setField(controller, "passwordEncoder", passwordEncoder);
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        stubLoginRecruiter(RecruiterStatus.RE_VERIFICATION, passwordEncoder);
        when(recruiterJwtService.issue(9L)).thenReturn("issued-token");

        mockMvc.perform(post("/api/jobs/recruiters/login")
                        .param("email", "recruiter@test.com")
                        .param("password", "secret"))
                .andExpect(status().isOk());
    }

    @Test
    void loginRecruiter_pendingStatus_returns401() throws Exception {
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        com.example.backend_service.TestUtils.setField(controller, "passwordEncoder", passwordEncoder);
        stubLoginRecruiter(RecruiterStatus.PENDING, passwordEncoder);

        mockMvc.perform(post("/api/jobs/recruiters/login")
                        .param("email", "recruiter@test.com")
                        .param("password", "secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRecruiter_rejectedStatus_returns401() throws Exception {
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        com.example.backend_service.TestUtils.setField(controller, "passwordEncoder", passwordEncoder);
        stubLoginRecruiter(RecruiterStatus.REJECTED, passwordEncoder);

        mockMvc.perform(post("/api/jobs/recruiters/login")
                        .param("email", "recruiter@test.com")
                        .param("password", "secret"))
                .andExpect(status().isUnauthorized());
    }

    // ---- self-service profile GET/PUT ----

    @Test
    void getOwnProfile_returnsAllDocumentUrls() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setCompanyName("Acme");
        r.setStatus(RecruiterStatus.VERIFIED);
        r.setOrgLogoUrl("https://blob/logo.png");
        r.setBusinessRegistrationUrl("https://blob/br.pdf");
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));

        String body = mockMvc.perform(get("/api/jobs/recruiters/me").header("X-Recruiter-Token", "valid-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecruiterProfileResponse profile = mapper.readValue(body, RecruiterProfileResponse.class);
        assertThat(profile.getOrgLogoUrl()).isEqualTo("https://blob/logo.png");
        assertThat(profile.getBusinessRegistrationUrl()).isEqualTo("https://blob/br.pdf");
        assertThat(profile.getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void updateOwnProfile_verifiedRecruiter_flipsToReVerification() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setCompanyName("Old Name");
        r.setContactPerson("Old Contact");
        r.setStatus(RecruiterStatus.VERIFIED);
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));
        when(recruiterRepository.save(any(Recruiter.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/jobs/recruiters/me")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("companyName", "New Name")
                        .param("contactPerson", "Old Contact"))
                .andExpect(status().isOk());

        assertThat(r.getCompanyName()).isEqualTo("New Name");
        assertThat(r.getStatus()).isEqualTo(RecruiterStatus.RE_VERIFICATION);
    }

    @Test
    void updateOwnProfile_noActualChange_statusUnchanged() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setCompanyName("Same Name");
        r.setContactPerson("Same Contact");
        r.setStatus(RecruiterStatus.VERIFIED);
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));
        when(recruiterRepository.save(any(Recruiter.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/jobs/recruiters/me")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("companyName", "Same Name")
                        .param("contactPerson", "Same Contact"))
                .andExpect(status().isOk());

        assertThat(r.getStatus()).isEqualTo(RecruiterStatus.VERIFIED);
    }

    @Test
    void updateOwnProfile_pendingOrRejectedRecruiter_returns403() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setStatus(RecruiterStatus.REJECTED);
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/jobs/recruiters/me")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("companyName", "New Name")
                        .param("contactPerson", "New Contact"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOwnProfile_partialFileReplacement_keepsUntouchedUrlsForFilesNotResent() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        com.example.backend_service.AzureBlobService azureBlobService =
                Mockito.mock(com.example.backend_service.AzureBlobService.class);
        com.example.backend_service.TestUtils.setField(controller, "azureBlobService", azureBlobService);
        when(azureBlobService.uploadFile(any())).thenReturn("https://blob/new-logo.png");

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setCompanyName("Acme");
        r.setContactPerson("Contact");
        r.setStatus(RecruiterStatus.VERIFIED);
        r.setOrgLogoUrl("https://blob/old-logo.png");
        r.setAuthLetterUrl("https://blob/old-letter.pdf");
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));
        when(recruiterRepository.save(any(Recruiter.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockMultipartFile newLogo = new org.springframework.mock.web.MockMultipartFile(
                "orgLogo", "logo.png", "image/png", "fake-bytes".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/jobs/recruiters/me")
                        .file(newLogo)
                        .header("X-Recruiter-Token", "valid-token")
                        .param("companyName", "Acme")
                        .param("contactPerson", "Contact"))
                .andExpect(status().isOk());

        // Only orgLogo was resent - it gets a new URL, authLetter (not resent) is untouched.
        assertThat(r.getOrgLogoUrl()).isEqualTo("https://blob/new-logo.png");
        assertThat(r.getAuthLetterUrl()).isEqualTo("https://blob/old-letter.pdf");
        assertThat(r.getStatus()).isEqualTo(RecruiterStatus.RE_VERIFICATION);
    }

    @Test
    void updateOwnProfile_noFileSent_leavesExistingDocumentUrlsUntouched() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(5L);

        Recruiter r = new Recruiter();
        r.setId(5L);
        r.setCompanyName("Acme");
        r.setContactPerson("Contact");
        r.setStatus(RecruiterStatus.VERIFIED);
        r.setOrgLogoUrl("https://blob/old-logo.png");
        r.setAuthLetterUrl("https://blob/old-letter.pdf");
        when(recruiterRepository.findById(5L)).thenReturn(Optional.of(r));
        when(recruiterRepository.save(any(Recruiter.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/jobs/recruiters/me")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("companyName", "New Acme Name")
                        .param("contactPerson", "Contact"))
                .andExpect(status().isOk());

        assertThat(r.getOrgLogoUrl()).isEqualTo("https://blob/old-logo.png");
        assertThat(r.getAuthLetterUrl()).isEqualTo("https://blob/old-letter.pdf");
        assertThat(r.getStatus()).isEqualTo(RecruiterStatus.RE_VERIFICATION);
    }

    // ---- report a job ----

    @Test
    void reportJob_createsReportAndDeactivatesJob() throws Exception {
        authenticateAsStudent(77L);

        Job job = new Job();
        job.setId(12L);
        job.setActive(true);
        when(jobRepository.findById(12L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobReportRepository.existsByJobIdAndReportedByStudentIdAndResolvedFalse(12L, 77L))
                .thenReturn(false);
        when(jobReportRepository.save(any(JobReport.class))).thenAnswer(inv -> inv.getArgument(0));

        JobReportRequest request = new JobReportRequest("Spam or scam");

        mockMvc.perform(post("/api/jobs/12/report")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(job.isUnderReview()).isTrue();
        assertThat(job.isActive()).isFalse();
        verify(jobReportRepository).save(any(JobReport.class));
    }

    @Test
    void reportJob_blankReason_returns400() throws Exception {
        authenticateAsStudent(77L);
        JobReportRequest request = new JobReportRequest("  ");

        mockMvc.perform(post("/api/jobs/12/report")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(jobReportRepository, never()).save(any(JobReport.class));
    }

    @Test
    void reportJob_alreadyReportedByThisStudent_doesNotCreateSecondReport() throws Exception {
        authenticateAsStudent(77L);

        Job job = new Job();
        job.setId(12L);
        when(jobRepository.findById(12L)).thenReturn(Optional.of(job));
        when(jobReportRepository.existsByJobIdAndReportedByStudentIdAndResolvedFalse(12L, 77L))
                .thenReturn(true);

        JobReportRequest request = new JobReportRequest("Spam or scam");

        mockMvc.perform(post("/api/jobs/12/report")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(jobReportRepository, never()).save(any(JobReport.class));
    }

    // ---- recruiter can't reactivate a reported/blocked job themselves ----

    @Test
    void setJobActive_underReview_rejectsReactivation() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(40L);

        Job job = new Job();
        job.setId(7L);
        job.setActive(false);
        job.setUnderReview(true);
        when(jobRepository.findByIdAndRecruiterId(7L, 40L)).thenReturn(Optional.of(job));

        mockMvc.perform(patch("/api/jobs/recruiters/40/jobs/7/active")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("active", "true"))
                .andExpect(status().isForbidden());

        assertThat(job.isActive()).isFalse();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void setJobActive_blocked_rejectsReactivation() throws Exception {
        RecruiterJwtService recruiterJwtService = Mockito.mock(RecruiterJwtService.class);
        com.example.backend_service.TestUtils.setField(controller, "recruiterJwtService", recruiterJwtService);
        when(recruiterJwtService.parse("valid-token")).thenReturn(40L);

        Job job = new Job();
        job.setId(7L);
        job.setActive(false);
        job.setBlocked(true);
        when(jobRepository.findByIdAndRecruiterId(7L, 40L)).thenReturn(Optional.of(job));

        mockMvc.perform(patch("/api/jobs/recruiters/40/jobs/7/active")
                        .header("X-Recruiter-Token", "valid-token")
                        .param("active", "true"))
                .andExpect(status().isForbidden());

        assertThat(job.isActive()).isFalse();
        verify(jobRepository, never()).save(any(Job.class));
    }

    // ---- admin: dismiss report / block job ----

    @Test
    void dismissReport_resolvesReportsAndReactivatesJob() throws Exception {
        Job job = new Job();
        job.setId(12L);
        job.setActive(false);
        job.setUnderReview(true);
        when(jobRepository.findById(12L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobReport report = new JobReport();
        report.setId(1L);
        report.setResolved(false);
        when(jobReportRepository.findByJobIdAndResolvedFalse(12L)).thenReturn(List.of(report));

        mockMvc.perform(put("/api/jobs/admin/12/dismiss-report"))
                .andExpect(status().isOk());

        assertThat(job.isUnderReview()).isFalse();
        assertThat(job.isActive()).isTrue();
        assertThat(report.isResolved()).isTrue();
        verify(jobReportRepository).saveAll(List.of(report));
    }

    @Test
    void blockJob_resolvesReportsAndBlocksJob() throws Exception {
        Job job = new Job();
        job.setId(12L);
        job.setActive(false);
        job.setUnderReview(true);
        when(jobRepository.findById(12L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobReport report = new JobReport();
        report.setId(1L);
        report.setResolved(false);
        when(jobReportRepository.findByJobIdAndResolvedFalse(12L)).thenReturn(List.of(report));

        mockMvc.perform(put("/api/jobs/admin/12/block"))
                .andExpect(status().isOk());

        assertThat(job.isUnderReview()).isFalse();
        assertThat(job.isBlocked()).isTrue();
        assertThat(job.isActive()).isFalse();
        assertThat(report.isResolved()).isTrue();
    }

    // ---- market trend ----
    // The actual computation (grouping, Gemini clustering, caching) now lives in
    // MarketTrendService - see MarketTrendServiceTest. This just confirms the controller
    // delegates to it rather than re-implementing anything itself.

    @Test
    void getMarketTrend_delegatesToMarketTrendService() throws Exception {
        com.example.backend_service.jobhub.service.MarketTrendService marketTrendService =
                Mockito.mock(com.example.backend_service.jobhub.service.MarketTrendService.class);
        com.example.backend_service.TestUtils.setField(controller, "marketTrendService", marketTrendService);

        List<com.example.backend_service.jobhub.dto.MarketTrendEntry> trend =
                List.of(new com.example.backend_service.jobhub.dto.MarketTrendEntry("Software Engineering Intern", 2));
        when(marketTrendService.getTrend()).thenReturn(trend);

        mockMvc.perform(get("/api/jobs/market-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Software Engineering Intern"))
                .andExpect(jsonPath("$[0].postingCount").value(2));
    }
}
