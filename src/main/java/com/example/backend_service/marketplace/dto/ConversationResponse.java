package com.example.backend_service.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Summary of a buyer-seller conversation about a listing, as shown in a conversation list.
 * Full message history is fetched separately via the messages endpoint.
 */
public class ConversationResponse {

    private Long id;
    private MarketplaceItemResponse item;
    private String buyerName;
    private String buyerEmail;
    private SellerResponse seller;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private boolean hasUnread;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketplaceItemResponse getItem() { return item; }
    public void setItem(MarketplaceItemResponse item) { this.item = item; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public SellerResponse getSeller() { return seller; }
    public void setSeller(SellerResponse seller) { this.seller = seller; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public boolean isHasUnread() { return hasUnread; }
    public void setHasUnread(boolean hasUnread) { this.hasUnread = hasUnread; }
}
