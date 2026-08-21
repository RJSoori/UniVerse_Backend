package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.ReportStatus;

import java.time.LocalDateTime;

/**
 * Data transfer object for a buyer's report of a marketplace listing, as shown
 * in the admin moderation queue.
 */
public class ListingReportResponse {

    private Long id;
    private MarketplaceItemResponse item;
    private String reportedByName;
    private String reportedByEmail;
    private String reason;
    private ReportStatus status;
    private LocalDateTime reportedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketplaceItemResponse getItem() { return item; }
    public void setItem(MarketplaceItemResponse item) { this.item = item; }
    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }
    public String getReportedByEmail() { return reportedByEmail; }
    public void setReportedByEmail(String reportedByEmail) { this.reportedByEmail = reportedByEmail; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
}
