package com.example.backend_service.marketplace.model;

import com.example.backend_service.marketplace.enums.SenderType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single message within a buyer-seller Conversation about a listing.
 */
@Entity
@Table(name = "marketplace_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Column(length = 2000)
    private String content;

    // Set when this message is an image attachment (Azure Blob URL) instead of, or
    // alongside, text content.
    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    public ChatMessage() {}

    @PrePersist
    private void onCreate() {
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public SenderType getSenderType() { return senderType; }
    public void setSenderType(SenderType senderType) { this.senderType = senderType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
