package com.example.backend_service.marketplace.service;

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.marketplace.dto.ChatMessageResponse;
import com.example.backend_service.marketplace.dto.ConversationResponse;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.enums.SenderType;
import com.example.backend_service.marketplace.model.ChatMessage;
import com.example.backend_service.marketplace.model.Conversation;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.model.Seller;
import com.example.backend_service.marketplace.repository.ChatMessageRepository;
import com.example.backend_service.marketplace.repository.ConversationRepository;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles buyer-seller chat about a listing. Each conversation is anchored to one item
 * and one buyer; a seller sees all conversations across their items.
 */
@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MarketplaceItemRepository itemRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Gets the buyer's existing conversation with this item's seller (regardless of
     * which item it started on), or starts a new one. Either way, the conversation's
     * item reference is updated to this item, so the chat's header context always
     * reflects whichever listing the buyer most recently opened the chat from.
     */
    public ConversationResponse startConversation(Long itemId, Long studentId) {
        MarketplaceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
        Student buyer = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found with id: " + studentId));
        Long sellerId = item.getSeller().getId();

        Conversation conversation = conversationRepository.findBySellerIdAndBuyerId(sellerId, studentId)
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setBuyer(buyer);
                    created.setSeller(item.getSeller());
                    return created;
                });
        conversation.setItem(item);
        conversation = conversationRepository.save(conversation);
        return toConversationResponse(conversation, true);
    }

    /**
     * Lists all conversations the given student has started, most recent first.
     */
    public List<ConversationResponse> getBuyerConversations(Long studentId) {
        return conversationRepository.findByBuyerIdOrderByIdDesc(studentId)
                .stream()
                .map(c -> toConversationResponse(c, true))
                .collect(Collectors.toList());
    }

    /**
     * Lists all conversations across every listing the given seller owns, most recent first.
     */
    public List<ConversationResponse> getSellerConversations(Long sellerId) {
        return conversationRepository.findBySellerIdOrderByIdDesc(sellerId)
                .stream()
                .map(c -> toConversationResponse(c, false))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the full message history for a conversation, and marks it as read for
     * whichever side is calling. Caller must be either the buyer (studentId) or the
     * seller (sellerId) on that conversation; pass null for whichever identity doesn't
     * apply to the current caller.
     */
    public List<ChatMessageResponse> getMessages(Long conversationId, Long studentId, Long sellerId) {
        Conversation conversation = requireParticipant(conversationId, studentId, sellerId);
        if (sellerId != null) {
            conversation.setSellerLastReadAt(java.time.LocalDateTime.now());
        } else {
            conversation.setBuyerLastReadAt(java.time.LocalDateTime.now());
        }
        conversationRepository.save(conversation);
        return chatMessageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId())
                .stream()
                .map(ChatService::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Sends a message into a conversation on behalf of whichever identity is calling
     * (buyer or seller). Pass null for whichever identity doesn't apply.
     */
    public ChatMessageResponse sendMessage(Long conversationId, Long studentId, Long sellerId, String content) {
        Conversation conversation = requireParticipant(conversationId, studentId, sellerId);
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderType(sellerId != null ? SenderType.SELLER : SenderType.STUDENT);
        message.setContent(content);
        return toMessageResponse(chatMessageRepository.save(message));
    }

    /**
     * Sends an image message into a conversation on behalf of whichever identity is
     * calling (buyer or seller). Pass null for whichever identity doesn't apply.
     */
    public ChatMessageResponse sendImageMessage(Long conversationId, Long studentId, Long sellerId, String imageUrl) {
        Conversation conversation = requireParticipant(conversationId, studentId, sellerId);
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderType(sellerId != null ? SenderType.SELLER : SenderType.STUDENT);
        message.setImageUrl(imageUrl);
        return toMessageResponse(chatMessageRepository.save(message));
    }

    private Conversation requireParticipant(Long conversationId, Long studentId, Long sellerId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found with id: " + conversationId));
        boolean isBuyer = studentId != null && studentId.equals(conversation.getBuyer().getId());
        boolean isSeller = sellerId != null && sellerId.equals(conversation.getSeller().getId());
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException();
        }
        return conversation;
    }

    private ConversationResponse toConversationResponse(Conversation conversation, boolean viewerIsBuyer) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setItem(conversation.getItem() != null ? toItemResponse(conversation.getItem()) : null);
        response.setBuyerName(conversation.getBuyer().getName());
        response.setBuyerEmail(conversation.getBuyer().getEmail());
        response.setSeller(toSellerResponse(conversation.getSeller()));
        ChatMessage lastMessage = chatMessageRepository.findFirstByConversationIdOrderBySentAtDesc(conversation.getId());
        if (lastMessage != null) {
            response.setLastMessage(lastMessage.getContent() != null ? lastMessage.getContent() : "📷 Photo");
            response.setLastMessageAt(lastMessage.getSentAt());

            SenderType otherPartyType = viewerIsBuyer ? SenderType.SELLER : SenderType.STUDENT;
            java.time.LocalDateTime myLastReadAt = viewerIsBuyer
                    ? conversation.getBuyerLastReadAt()
                    : conversation.getSellerLastReadAt();
            boolean isFromOtherParty = lastMessage.getSenderType() == otherPartyType;
            boolean isUnread = myLastReadAt == null || lastMessage.getSentAt().isAfter(myLastReadAt);
            response.setHasUnread(isFromOtherParty && isUnread);
        }
        return response;
    }

    private static ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSenderType(message.getSenderType());
        response.setContent(message.getContent());
        response.setImageUrl(message.getImageUrl());
        response.setSentAt(message.getSentAt());
        return response;
    }

    private static MarketplaceItemResponse toItemResponse(MarketplaceItem item) {
        MarketplaceItemResponse response = new MarketplaceItemResponse();
        response.setId(item.getId());
        response.setItemName(item.getItemName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setType(item.getType());
        response.setCondition(item.getCondition());
        response.setStatus(item.getStatus());
        response.setImageUrl(item.getImageUrl());
        response.setImageUrls(item.getImageUrls());
        response.setCategory(item.getCategory());
        response.setViewCount(item.getViewCount());
        response.setTotalUnits(item.getTotalUnits());
        response.setSoldUnits(item.getSoldUnits());
        response.setTimesRented(item.getTimesRented());
        if (item.getSeller() != null) {
            response.setSeller(toSellerResponse(item.getSeller()));
        }
        return response;
    }

    private static SellerResponse toSellerResponse(Seller seller) {
        SellerResponse response = new SellerResponse();
        response.setId(seller.getId());
        response.setStoreName(seller.getStoreName());
        response.setEmail(seller.getEmail());
        response.setPhone(seller.getPhone());
        response.setDescription(seller.getDescription());
        response.setStatus(seller.getStatus() != null ? seller.getStatus().name() : null);
        response.setRegisteredAt(seller.getRegisteredAt());
        return response;
    }
}
