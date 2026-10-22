package com.example.backend_service.marketplace.model;

import com.example.backend_service.marketplace.enums.ReportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a seller's request to be re-verified (e.g. after changing store details
 * or identity documents). Reviewed by an admin in the moderation queue, mirroring
 * {@link ListingReport}.
 */
@Entity
@Table(name = "seller_reverification_requests")
public class SellerReverificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    public SellerReverificationRequest() {}

    @PrePersist
    private void onCreate() {
        this.requestedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
