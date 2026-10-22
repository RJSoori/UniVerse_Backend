package com.example.backend_service.marketplace.model;

import com.example.backend_service.Student;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A chat thread between a buyer and a seller. A given buyer has at most one
 * conversation per seller, shared across every listing they discuss — {@link #item}
 * tracks whichever listing was most recently opened from, for context display, but
 * doesn't partition the thread.
 */
@Entity
@Table(name = "marketplace_conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "buyer_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private MarketplaceItem item;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private Student buyer;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // When each side last viewed this conversation's messages — used to compute
    // whether they have unread messages from the other party.
    private LocalDateTime buyerLastReadAt;
    private LocalDateTime sellerLastReadAt;

    public Conversation() {}

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketplaceItem getItem() { return item; }
    public void setItem(MarketplaceItem item) { this.item = item; }
    public Student getBuyer() { return buyer; }
    public void setBuyer(Student buyer) { this.buyer = buyer; }
    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getBuyerLastReadAt() { return buyerLastReadAt; }
    public void setBuyerLastReadAt(LocalDateTime buyerLastReadAt) { this.buyerLastReadAt = buyerLastReadAt; }
    public LocalDateTime getSellerLastReadAt() { return sellerLastReadAt; }
    public void setSellerLastReadAt(LocalDateTime sellerLastReadAt) { this.sellerLastReadAt = sellerLastReadAt; }
}
