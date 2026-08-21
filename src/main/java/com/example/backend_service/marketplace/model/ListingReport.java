package com.example.backend_service.marketplace.model;

import com.example.backend_service.Student;
import com.example.backend_service.marketplace.enums.ReportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a buyer's report of a marketplace listing (e.g. inappropriate content,
 * spam, fraudulent listing). Reviewed by an admin in the moderation queue.
 */
@Entity
@Table(name = "marketplace_item_reports")
public class ListingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private MarketplaceItem item;

    @ManyToOne
    @JoinColumn(name = "reported_by_student_id", nullable = false)
    private Student reportedBy;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    // Set once, at the moment a resolveReport() call flips this report (and every other
    // currently-open report on the same listing) to RESOLVED together as one incident.
    // Lets reinstateListing scope a reversal to exactly that incident's reports, rather
    // than every report that happens to currently sit at RESOLVED for the listing — which
    // could otherwise span multiple separate, unrelated takedown incidents over time.
    private LocalDateTime resolvedAt;

    public ListingReport() {}

    @PrePersist
    private void onCreate() {
        this.reportedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketplaceItem getItem() { return item; }
    public void setItem(MarketplaceItem item) { this.item = item; }
    public Student getReportedBy() { return reportedBy; }
    public void setReportedBy(Student reportedBy) { this.reportedBy = reportedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
