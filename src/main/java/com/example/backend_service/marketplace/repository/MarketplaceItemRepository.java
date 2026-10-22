package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.enums.ItemStatus;
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
     * Retrieves all items listed by a specific seller. Used internally where display
     * order doesn't matter (counting, status sweeps) — for a seller's inventory or store
     * page, use findBySellerIdOrderByIdDesc instead.
     */
    List<MarketplaceItem> findBySellerId(Long sellerId);

    /**
     * Retrieves all items listed by a specific seller, newest first. Used to display a
     * seller's inventory in their dashboard or public store page.
     */
    List<MarketplaceItem> findBySellerIdOrderByIdDesc(Long sellerId);

    /**
     * Retrieves all items with the given status, newest first. Used for the admin
     * moderation queue (PENDING_APPROVAL).
     */
    List<MarketplaceItem> findByStatusOrderByIdDesc(ItemStatus status);

    /**
     * Retrieves all items whose status is not one of the given values, newest first.
     * Used to exclude PENDING_APPROVAL/REJECTED/etc. items from public browsing.
     */
    List<MarketplaceItem> findByStatusNotInOrderByIdDesc(List<ItemStatus> statuses);
}