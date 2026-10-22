package com.example.backend_service.auth.repository;

import com.example.backend_service.auth.model.StudentEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentEmailVerificationRepository extends JpaRepository<StudentEmailVerification, String> {
}
