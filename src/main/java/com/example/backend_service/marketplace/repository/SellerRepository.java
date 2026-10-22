package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Seller entities.
 * Provides methods to query sellers by unique identifiers for authentication and profile retrieval.
 */
@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    /**
     * Finds a seller by their registered email address.
     * Used during registration to prevent duplicate email registrations.
     */
    Optional<Seller> findByEmail(String email);
    
    /**
     * Finds a seller by their username.
     * Used during login to authenticate seller credentials.
     */
    Optional<Seller> findByUsername(String username);

    /**
     * Retrieves every seller, most recently registered first. Used for the admin
     * verification/registered-accounts queues.
     */
    List<Seller> findAllByOrderByRegisteredAtDesc();
}