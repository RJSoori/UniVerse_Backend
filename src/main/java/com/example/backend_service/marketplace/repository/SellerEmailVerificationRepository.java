package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.model.SellerEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerEmailVerificationRepository extends JpaRepository<SellerEmailVerification, String> {
}
