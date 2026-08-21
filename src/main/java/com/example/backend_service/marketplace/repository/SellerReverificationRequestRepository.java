package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.enums.ReportStatus;
import com.example.backend_service.marketplace.model.SellerReverificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for SellerReverificationRequest entities.
 */
@Repository
public interface SellerReverificationRequestRepository extends JpaRepository<SellerReverificationRequest, Long> {
    /**
     * Retrieves all requests with the given status, newest first. Used for the admin
     * moderation queue.
     */
    List<SellerReverificationRequest> findByStatusOrderByRequestedAtDesc(ReportStatus status);
}
