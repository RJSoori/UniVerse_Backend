package com.example.backend_service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobHubController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private CloudStorageService cloudStorageService;

    // --- Recruiter Endpoints ---
    @PostMapping("/recruiters")
    public Recruiter registerRecruiter(@RequestBody Recruiter recruiter) {
        return recruiterRepository.save(recruiter);
    }

    @PostMapping(value = "/recruiters/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Recruiter registerRecruiterWithDocuments(
            @RequestParam String companyName,
            @RequestParam String email,
            @RequestParam String contactPerson,
            @RequestPart(required = false) MultipartFile businessRegistration,
            @RequestPart(required = false) MultipartFile orgLogo,
            @RequestPart(required = false) MultipartFile authLetter) {

        Recruiter recruiter = new Recruiter();
        recruiter.setCompanyName(companyName);
        recruiter.setEmail(email);
        recruiter.setContactPerson(contactPerson);

        Recruiter saved = recruiterRepository.save(recruiter);

        if (businessRegistration != null && !businessRegistration.isEmpty()) {
            saved.setBusinessRegistrationUrl(cloudStorageService.uploadDocument(businessRegistration, saved.getId(), "business-registration"));
        }
        if (orgLogo != null && !orgLogo.isEmpty()) {
            saved.setOrgLogoUrl(cloudStorageService.uploadDocument(orgLogo, saved.getId(), "org-logo"));
        }
        if (authLetter != null && !authLetter.isEmpty()) {
            saved.setAuthLetterUrl(cloudStorageService.uploadDocument(authLetter, saved.getId(), "auth-letter"));
        }

        return recruiterRepository.save(saved);
    }

    @GetMapping("/recruiters")
    public List<Recruiter> getAllRecruiters() {
        return recruiterRepository.findAll();
    }

    @GetMapping("/recruiters/{email}")
    public Recruiter getRecruiterByEmail(@PathVariable String email) {
        return recruiterRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
    }

    @PutMapping("/recruiters/{id}/verify")
    public Recruiter verifyRecruiter(@PathVariable Long id, @RequestParam String status) {
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        recruiter.setStatus(Recruiter.VerificationStatus.valueOf(status));
        return recruiterRepository.save(recruiter);
    }

    // --- Job Endpoints ---
    @PostMapping("/post")
    public Job postJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @GetMapping("/all")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // --- Application Endpoints ---
    @PostMapping("/apply")
    public JobApplication applyToJob(@RequestBody JobApplication application) {
        return applicationRepository.save(application);
    }

    @GetMapping("/applications/{jobId}")
    public List<JobApplication> getApplicationsByJob(@PathVariable Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    @GetMapping("/applications/student/{email}")
    public List<JobApplication> getApplicationsByStudent(@PathVariable String email) {
        return applicationRepository.findByStudentEmail(email);
    }

    @PutMapping("/applications/{id}/status")
    public JobApplication updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        app.setStatus(JobApplication.ApplicationStatus.valueOf(status));
        return applicationRepository.save(app);
    }
}
