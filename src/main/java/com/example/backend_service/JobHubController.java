package com.example.backend_service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    // --- Recruiter Endpoints ---
    @PostMapping("/recruiters/login")
    public Recruiter loginRecruiter(@RequestParam("email") String email, @RequestParam("password") String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Recruiter recruiter = recruiterRepository.findByEmail(normalizedEmail);
        if (recruiter != null && passwordEncoder.matches(password, recruiter.getPassword())) {
            return recruiter;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
    }

    @PostMapping("/recruiters")
    public Recruiter registerRecruiter(@RequestParam("companyName") String companyName,
                                       @RequestParam("email") String email,
                                       @RequestParam("contactPerson") String contactPerson,
                                       @RequestParam("password") String password,
                                       @RequestParam(value = "accountType", defaultValue = "company") String accountType,
                                       @RequestParam(value = "businessRegistration", required = false) MultipartFile businessRegistration,
                                       @RequestParam(value = "orgLogo", required = false) MultipartFile orgLogo,
                                       @RequestParam(value = "authLetter", required = false) MultipartFile authLetter) throws IOException {
        Recruiter recruiter = new Recruiter();
        recruiter.setCompanyName(companyName);
        recruiter.setEmail(email.trim().toLowerCase());
        recruiter.setContactPerson(contactPerson);
        recruiter.setPassword(passwordEncoder.encode(password));
        recruiter.setAccountType(accountType);

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

    // --- Job Endpoints ---
    @PostMapping("/post")
    public Job postJob(@RequestBody Job job) {
        // Check if recruiter is verified
        Recruiter recruiter = recruiterRepository.findById(job.getRecruiter().getId())
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
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

    // --- Admin Endpoints ---
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
