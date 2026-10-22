package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.ReportStatus;

import java.time.LocalDateTime;

/**
 * Data transfer object for a seller's request to be re-verified (e.g. after changing
 * store details or identity), as shown in the admin moderation queue.
 */
public class SellerReverificationRequestResponse {

    private Long id;
    private SellerResponse seller;
    private String reason;
    private ReportStatus status;
    private LocalDateTime requestedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SellerResponse getSeller() { return seller; }
    public void setSeller(SellerResponse seller) { this.seller = seller; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
