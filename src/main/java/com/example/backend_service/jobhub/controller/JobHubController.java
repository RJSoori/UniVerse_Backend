package com.example.backend_service.jobhub.controller;

import java.io.IOException;
import java.util.List;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend_service.AzureBlobService;
import com.example.backend_service.RecruiterStatus;
import com.example.backend_service.common.exception.UnauthorizedException;
import com.example.backend_service.jobhub.dto.JobRequest;
import com.example.backend_service.jobhub.enums.JobStatus;
import com.example.backend_service.jobhub.model.Job;
import com.example.backend_service.jobhub.model.Recruiter;
import com.example.backend_service.jobhub.repository.JobRepository;
import com.example.backend_service.jobhub.repository.RecruiterRepository;

@RestController
@RequestMapping("/api/jobs")
public class JobHubController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private AzureBlobService azureBlobService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Recruiter Endpoints
    @PostMapping("/recruiters/login")
    public Recruiter loginRecruiter(@RequestParam("email") String email, @RequestParam("password") String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Recruiter recruiter = recruiterRepository.findByEmail(normalizedEmail);
        if (recruiter != null && passwordEncoder.matches(password, recruiter.getPassword())) {
            if (recruiter.getStatus() != RecruiterStatus.VERIFIED) {
                throw new UnauthorizedException("Account not verified");
            }
            return recruiter;
        }
        throw new UnauthorizedException("Invalid credentials");
    }

    @PostMapping("/recruiters")
    public Recruiter registerRecruiter(
            @RequestParam("companyName") String companyName,
            @RequestParam("email") String email,
            @RequestParam("contactPerson") String contactPerson,
            @RequestParam("password") String password,
            @RequestParam(value = "accountType", defaultValue = "company") String accountType,
            @RequestParam(value = "businessRegistration", required = false) MultipartFile businessRegistration,
            @RequestParam(value = "orgLogo", required = false) MultipartFile orgLogo,
            @RequestParam(value = "authLetter", required = false) MultipartFile authLetter,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestParam(value = "idDocument", required = false) MultipartFile idDocument
    ) throws IOException {
        Recruiter recruiter = new Recruiter();
        recruiter.setCompanyName(companyName);
        recruiter.setEmail(email.trim().toLowerCase());
        recruiter.setContactPerson(contactPerson);
        recruiter.setPassword(passwordEncoder.encode(password));
        recruiter.setAccountType(accountType);

        // Corporate documents
        if (businessRegistration != null && !businessRegistration.isEmpty()) {
            String url = azureBlobService.uploadFile(businessRegistration);
            recruiter.setBusinessRegistrationUrl(url);
        }
        if (orgLogo != null && !orgLogo.isEmpty()) {
            String url = azureBlobService.uploadFile(orgLogo);
            recruiter.setOrgLogoUrl(url);
        }
        if (authLetter != null && !authLetter.isEmpty()) {
            String url = azureBlobService.uploadFile(authLetter);
            recruiter.setAuthLetterUrl(url);
        }

        // Individual documents
        if (profilePicture != null && !profilePicture.isEmpty()) {
            String url = azureBlobService.uploadFile(profilePicture);
            recruiter.setProfilePictureUrl(url);
        }
        if (idDocument != null && !idDocument.isEmpty()) {
            String url = azureBlobService.uploadFile(idDocument);
            recruiter.setIdDocumentUrl(url);
        }

        return recruiterRepository.save(recruiter);
    }

    @GetMapping("/recruiters")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Recruiter> getAllRecruiters() {
        return recruiterRepository.findAll();
    }

    @PutMapping("/recruiters/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public Recruiter verifyRecruiter(@PathVariable Long id, @RequestParam String status) {
        Recruiter recruiter = recruiterRepository.findById(id).orElseThrow(() -> new RuntimeException("Recruiter not found"));
        recruiter.setStatus(RecruiterStatus.valueOf(status));
        return recruiterRepository.save(recruiter);
    }

    // Job Endpoints
    @PostMapping("/post")
    public Job postJob(@RequestBody JobRequest request) {
        Recruiter recruiter = recruiterRepository.findById(request.recruiterId())
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
        if (recruiter.getStatus() == RecruiterStatus.VERIFIED) {
            job.setStatus(JobStatus.APPROVED);
        } else {
            job.setStatus(JobStatus.PENDING);
        }
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
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecruiterJob(@PathVariable Long recruiterId, @PathVariable Long jobId) {
        Job job = jobRepository.findByIdAndRecruiterId(jobId, recruiterId)
                .orElseThrow(() -> new RuntimeException("Job not found or unauthorized deletion attempt"));
        jobRepository.delete(job);
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
}
