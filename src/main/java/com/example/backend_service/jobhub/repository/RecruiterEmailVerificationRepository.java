package com.example.backend_service.jobhub.repository;

import com.example.backend_service.jobhub.model.RecruiterEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruiterEmailVerificationRepository extends JpaRepository<RecruiterEmailVerification, String> {
}
