package com.example.backend_service.jobhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend_service.jobhub.model.Recruiter;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    Recruiter findByEmail(String email);
}