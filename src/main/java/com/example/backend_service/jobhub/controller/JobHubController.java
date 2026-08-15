package com.example.backend_service.jobhub.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend_service.AzureBlobService;
import com.example.backend_service.RecruiterStatus;
import com.example.backend_service.common.exception.UnauthorizedException;
import com.example.backend_service.common.dto.ForgotPasswordRequest;
import com.example.backend_service.jobhub.dto.JobRequest;
import com.example.backend_service.jobhub.dto.RecruiterAuthResponse;
import com.example.backend_service.common.dto.ResetPasswordRequest;
import com.example.backend_service.common.dto.SendEmailVerificationRequest;
import com.example.backend_service.common.dto.VerifyEmailRequest;
import com.example.backend_service.common.dto.VerifyEmailResponse;
import com.example.backend_service.common.dto.VerifyResetCodeRequest;
import com.example.backend_service.common.dto.VerifyResetCodeResponse;
import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.model.Recruiter;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.repository.RecruiterRepository;
import com.example.backend_service.jobhub.service.RecruiterEmailVerificationService;
import com.example.backend_service.jobhub.service.RecruiterJwtService;
import com.example.backend_service.jobhub.service.RecruiterPasswordResetService;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobHubController {

    private static final Logger log = LoggerFactory.getLogger(JobHubController.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private AzureBlobService azureBlobService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RecruiterJwtService recruiterJwtService;

    @Autowired
    private RecruiterPasswordResetService recruiterPasswordResetService;

    @Autowired
    private RecruiterEmailVerificationService recruiterEmailVerificationService;

    // Recruiter Endpoints
    @PostMapping("/recruiters/login")
    public RecruiterAuthResponse loginRecruiter(
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Recruiter recruiter = recruiterRepository.findByEmail(normalizedEmail);
        if (recruiter == null) {
            log.info("Login attempt for unknown recruiter email={}", normalizedEmail);
            throw new UnauthorizedException("Invalid credentials");
        }
        boolean passMatches = passwordEncoder.matches(password, recruiter.getPassword());
        log.info("Recruiter login attempt email={}, status={}, passMatches={}", normalizedEmail, recruiter.getStatus(), passMatches);
        if (!passMatches) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (recruiter.getStatus() != RecruiterStatus.VERIFIED) {
            throw new UnauthorizedException("Account not verified");
        }
        String token = recruiterJwtService.issue(recruiter.getId());
        return toAuthResponse(token, recruiter);
    }

    @PostMapping("/recruiters/email/send-code")
    public Map<String, String> sendRegistrationEmailCode(@RequestBody SendEmailVerificationRequest request) {
        recruiterEmailVerificationService.sendCode(request.email());
        return Map.of("message", "Verification code sent.");
    }

    @PostMapping("/recruiters/email/verify-code")
    public VerifyEmailResponse verifyRegistrationEmailCode(@RequestBody VerifyEmailRequest request) {
        String token = recruiterEmailVerificationService.verifyCode(request.email(), request.code());
        return new VerifyEmailResponse(token);
    }

    @PostMapping("/recruiters")
    public RecruiterAuthResponse registerRecruiter(
            @RequestParam("companyName") String companyName,
            @RequestParam("email") String email,
            @RequestParam("contactPerson") String contactPerson,
            @RequestParam("password") String password,
            @RequestParam(value = "accountType", defaultValue = "company") String accountType,
            @RequestParam("emailVerificationToken") String emailVerificationToken,
            @RequestParam(value = "businessRegistration", required = false) MultipartFile businessRegistration,
            @RequestParam(value = "orgLogo", required = false) MultipartFile orgLogo,
            @RequestParam(value = "authLetter", required = false) MultipartFile authLetter,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestParam(value = "idDocument", required = false) MultipartFile idDocument
    ) throws IOException {
        String normalizedEmail = email.trim().toLowerCase();
        Recruiter existing = recruiterRepository.findByEmail(normalizedEmail);
        if (existing != null) {
            throw new UnauthorizedException("Email already registered");
        }
        // Registration is only allowed once the email has been verified via the
        // send-code/verify-code pair above; this consumes (and single-uses) that token.
        recruiterEmailVerificationService.consumeVerification(normalizedEmail, emailVerificationToken);

        Recruiter recruiter = new Recruiter();
        recruiter.setCompanyName(companyName);
        recruiter.setEmail(normalizedEmail);
        recruiter.setContactPerson(contactPerson);
        recruiter.setPassword(passwordEncoder.encode(password));
        recruiter.setAccountType(accountType);

        if (businessRegistration != null && !businessRegistration.isEmpty()) {
            recruiter.setBusinessRegistrationUrl(azureBlobService.uploadFile(businessRegistration));
        }
        if (orgLogo != null && !orgLogo.isEmpty()) {
            recruiter.setOrgLogoUrl(azureBlobService.uploadFile(orgLogo));
        }
        if (authLetter != null && !authLetter.isEmpty()) {
            recruiter.setAuthLetterUrl(azureBlobService.uploadFile(authLetter));
        }
        if (profilePicture != null && !profilePicture.isEmpty()) {
            recruiter.setProfilePictureUrl(azureBlobService.uploadFile(profilePicture));
        }
        if (idDocument != null && !idDocument.isEmpty()) {
            recruiter.setIdDocumentUrl(azureBlobService.uploadFile(idDocument));
        }

        Recruiter saved = recruiterRepository.save(recruiter);
        // New registrations start as PENDING — no token issued until admin verifies
        return toAuthResponse(null, saved);
    }

    @PostMapping("/recruiters/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        recruiterPasswordResetService.requestReset(request.email());
        // Always the same response, whether or not the email matched an account, so we don't
        // leak which addresses are registered.
        return Map.of("message", "If that email is registered, we've sent a verification code to it.");
    }

    @PostMapping("/recruiters/verify-reset-code")
    public VerifyResetCodeResponse verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        String resetToken = recruiterPasswordResetService.verifyCode(request.email(), request.code());
        return new VerifyResetCodeResponse(resetToken);
    }

    @PostMapping("/recruiters/reset-password")
    public Map<String, String> resetPassword(@RequestBody ResetPasswordRequest request) {
        recruiterPasswordResetService.resetPassword(request.email(), request.resetToken(), request.newPassword());
        return Map.of("message", "Password updated successfully. You can now log in.");
    }

    @GetMapping("/recruiters")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Recruiter> getAllRecruiters() {
        return recruiterRepository.findAll();
    }

    @PutMapping("/recruiters/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public Recruiter verifyRecruiter(@PathVariable Long id, @RequestParam String status) {
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        recruiter.setStatus(RecruiterStatus.valueOf(status));
        return recruiterRepository.save(recruiter);
    }

    // Job Endpoints
    @PostMapping("/post")
    @PreAuthorize("hasRole('RECRUITER')")
    public Job postJob(
            @RequestHeader("X-Recruiter-Token") String recruiterToken,
            @RequestBody JobRequest request) {
        Long authRecruiterId = recruiterJwtService.parse(recruiterToken);
        Recruiter recruiter = recruiterRepository.findById(authRecruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        Job job = new Job();
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setSkills(request.skills());
        job.setSalaryInfo(request.salaryInfo());
        job.setWorkType(request.workType());
        job.setEmploymentType(request.employmentType());
        job.setPostedAt(request.postedAt());
        job.setRecruiter(recruiter);
        job.setStatus(recruiter.getStatus() == RecruiterStatus.VERIFIED ? JobStatus.APPROVED : JobStatus.PENDING);
        return jobRepository.save(job);
    }

    @GetMapping("/all")
    public List<Job> getAllJobs() {
        return jobRepository.findByStatus(JobStatus.APPROVED);
    }

    @GetMapping("/recruiters/{id}/jobs")
    public List<Job> getRecruiterJobs(@PathVariable Long id) {
        return jobRepository.findByRecruiterId(id);
    }

    @DeleteMapping("/recruiters/{recruiterId}/jobs/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecruiterJob(
            @RequestHeader("X-Recruiter-Token") String recruiterToken,
            @PathVariable Long recruiterId,
            @PathVariable Long jobId) {
        Long authRecruiterId = recruiterJwtService.parse(recruiterToken);
        log.info("Delete job request: authRecruiterId={}, jobId={}", authRecruiterId, jobId);
        Job job = jobRepository.findByIdAndRecruiterId(jobId, authRecruiterId)
                .orElseThrow(() -> {
                    log.error("Job not found or not owned: jobId={}, recruiterId={}", jobId, authRecruiterId);
                    return new RuntimeException("Job not found or unauthorized deletion attempt");
                });
        jobRepository.delete(job);
        jobRepository.flush();
        log.info("Job deleted successfully: id={}", jobId);
    }

    // Admin Endpoints
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Job> getPendingJobs() {
        return jobRepository.findByStatus(JobStatus.PENDING);
    }

    @PutMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Job approveJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus(JobStatus.APPROVED);
        return jobRepository.save(job);
    }

    @PutMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Job rejectJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus(JobStatus.REJECTED);
        return jobRepository.save(job);
    }

    private RecruiterAuthResponse toAuthResponse(String token, Recruiter recruiter) {
        RecruiterAuthResponse response = new RecruiterAuthResponse();
        response.setToken(token);
        response.setId(recruiter.getId());
        response.setCompanyName(recruiter.getCompanyName());
        response.setEmail(recruiter.getEmail());
        response.setContactPerson(recruiter.getContactPerson());
        response.setAccountType(recruiter.getAccountType());
        response.setStatus(recruiter.getStatus() != null ? recruiter.getStatus().name() : null);
        return response;
    }
}
