package com.example.backend_service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SellerRepository extends JpaRepository<Seller, Long> {}

@Repository
interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, Long> {}