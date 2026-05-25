package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.model.MarketplaceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for MarketplaceItem entities.
 * Provides methods to query items with filtering by seller and other criteria.
 */
@Repository
public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, Long> {
    /**
     * Retrieves all items listed by a specific seller.
     * Used to display a seller's inventory in their profile or store page.
     */
    List<MarketplaceItem> findBySellerId(Long sellerId);
}