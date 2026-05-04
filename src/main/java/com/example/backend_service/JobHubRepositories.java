package com.example.backend_service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    Recruiter findByEmail(String email);
}

@Repository
interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
}
