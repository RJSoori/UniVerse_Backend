package com.example.backend_service.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend_service.marketplace.model.Seller;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {}