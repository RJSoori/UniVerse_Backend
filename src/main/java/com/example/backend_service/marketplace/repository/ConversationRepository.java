package com.example.backend_service.marketplace.repository;

import com.example.backend_service.marketplace.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findBySellerIdAndBuyerId(Long sellerId, Long buyerId);

    List<Conversation> findByItemId(Long itemId);

    List<Conversation> findByBuyerIdOrderByIdDesc(Long buyerId);

    List<Conversation> findBySellerIdOrderByIdDesc(Long sellerId);
}
