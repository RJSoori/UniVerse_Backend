package com.example.backend_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*") // Allows your React frontend to connect
public class JobHubController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    // --- Recruiter Endpoints ---
    @PostMapping("/recruiters")
    public Recruiter registerRecruiter(@RequestBody Recruiter recruiter) {
        return recruiterRepository.save(recruiter);
    }

    @GetMapping("/recruiters")
    public List<Recruiter> getAllRecruiters() {
        return recruiterRepository.findAll();
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
}
