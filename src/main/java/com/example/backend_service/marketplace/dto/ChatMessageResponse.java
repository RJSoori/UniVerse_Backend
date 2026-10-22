package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.SenderType;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private SenderType senderType;
    private String content;
    private String imageUrl;
    private LocalDateTime sentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SenderType getSenderType() { return senderType; }
    public void setSenderType(SenderType senderType) { this.senderType = senderType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
