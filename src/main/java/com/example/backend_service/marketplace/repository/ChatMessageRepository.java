package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);

    List<ChatMessage> findByConversation_ItemId(Long itemId);

    ChatMessage findFirstByConversationIdOrderBySentAtDesc(Long conversationId);
}
