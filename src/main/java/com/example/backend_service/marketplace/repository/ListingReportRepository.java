package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.enums.ReportStatus;
import com.example.backend_service.marketplace.model.ListingReport;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for ListingReport entities.
 */
@Repository
public interface ListingReportRepository extends JpaRepository<ListingReport, Long> {
    /**
     * Retrieves all reports with the given status, newest first. Used for the admin
     * moderation queue.
     */
    List<ListingReport> findByStatusOrderByReportedAtDesc(ReportStatus status);

    /**
     * Retrieves all reports with any of the given statuses, newest first. Used for the
     * admin takedown history view (resolved + dismissed reports).
     */
    List<ListingReport> findByStatusInOrderByReportedAtDesc(List<ReportStatus> statuses);

    /**
     * Retrieves all reports referencing a given item. Used to clear reports before
     * a hard delete, since the FK to the item is required.
     */
    List<ListingReport> findByItem(MarketplaceItem item);
}
