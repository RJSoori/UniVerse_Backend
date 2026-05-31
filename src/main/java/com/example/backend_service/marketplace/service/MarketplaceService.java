package com.example.backend_service.marketplace.service;

import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.dto.SellerUpdateRequest;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.model.Seller;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import com.example.backend_service.marketplace.repository.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    @Autowired
    private MarketplaceItemRepository itemRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private SellerJwtService sellerJwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Creates a new seller account after validating that email and username are not already in use.
     * Encodes password using BCrypt for secure storage and issues a JWT token for immediate login.
     * Throws RuntimeException if email or username is already registered.
     */
    public SellerAuthResponse registerSeller(SellerRequest request) {
        if (sellerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        if (sellerRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }
        Seller seller = new Seller();
        seller.setStoreName(request.getStoreName());
        seller.setEmail(request.getEmail());
        seller.setUsername(request.getUsername());
        seller.setPassword(passwordEncoder.encode(request.getPassword()));
        seller.setPhone(request.getPhone());
        seller.setDescription(request.getDescription());
        Seller saved = sellerRepository.save(seller);
        String token = sellerJwtService.issue(saved.getId());
        return new SellerAuthResponse(token, toSellerResponse(saved));
    }

    /**
     * Authenticates a seller by finding them by username and verifying the password.
     * Issues a JWT token upon successful authentication for use in subsequent requests.
     * Throws RuntimeException if username doesn't exist or password is incorrect.
     */
    public SellerAuthResponse loginSeller(SellerLoginRequest request) {
        Seller seller = sellerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), seller.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        String token = sellerJwtService.issue(seller.getId());
        return new SellerAuthResponse(token, toSellerResponse(seller));
    }

    /**
     * Extracts seller ID from a JWT token and retrieves the corresponding seller profile.
     * Used internally for token-based seller lookups.
     */
    public SellerResponse getSellerByToken(String token) {
        Long sellerId = sellerJwtService.parse(token);
        return getSellerById(sellerId);
    }

    /**
     * Retrieves all sellers registered in the system and converts them to response DTOs.
     */
    public List<SellerResponse> getAllSellers() {
        return sellerRepository.findAll()
                .stream()
                .map(this::toSellerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific seller by ID. Throws NotFoundException if seller doesn't exist.
     */
    public SellerResponse getSellerById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + id));
        return toSellerResponse(seller);
    }

    /**
 * Updates an existing seller's profile information.
 * Only updates fields that are provided (not null).
 * Throws NotFoundException if seller doesn't exist.
 */
public SellerResponse updateSeller(Long sellerId, SellerUpdateRequest request) {
    Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
    if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
        seller.setStoreName(request.getStoreName());
    }
    if (request.getPhone() != null && !request.getPhone().isBlank()) {
        seller.setPhone(request.getPhone());
    }
    if (request.getDescription() != null) {
        seller.setDescription(request.getDescription());
    }
    Seller updated = sellerRepository.save(seller);
    return toSellerResponse(updated);
}

    /**
     * Creates a new marketplace item after validating that the seller exists.
     * Sets initial status to ACTIVE and associates the item with the selling seller.
     * Throws NotFoundException if seller ID doesn't exist in the system.
     */
    public MarketplaceItemResponse createItem(MarketplaceItemRequest request) {
        Seller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + request.getSellerId()));
        MarketplaceItem item = new MarketplaceItem();
        item.setItemName(request.getItemName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setType(request.getType());
        item.setCondition(request.getCondition());
        item.setImageUrl(request.getImageUrl());
        item.setSeller(seller);
        MarketplaceItem saved = itemRepository.save(item);
        return toItemResponse(saved);
    }

    /**
     * Retrieves all active and inactive marketplace items available in the system.
     * Converts each item to a response DTO including seller information.
     */
    public List<MarketplaceItemResponse> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all items listed by a specific seller, filtered by seller ID.
     * Returns empty list if seller has no items.
     */
    public List<MarketplaceItemResponse> getItemsBySeller(Long sellerId) {
        return itemRepository.findBySellerId(sellerId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves details of a specific marketplace item by ID.
     * Throws NotFoundException if item doesn't exist.
     */
    public MarketplaceItemResponse getItemById(Long id) {
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        return toItemResponse(item);
    }

    /**
     * Permanently removes a marketplace item from the system.
     * Validates that item exists before attempting deletion.
     * Throws NotFoundException if item ID doesn't exist.
     */
    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    /**
     * Converts a Seller entity to a SellerResponse DTO, excluding sensitive information like password.
     * Maps relevant seller information for API responses.
     */
    private SellerResponse toSellerResponse(Seller seller) {
        SellerResponse response = new SellerResponse();
        response.setId(seller.getId());
        response.setStoreName(seller.getStoreName());
        response.setEmail(seller.getEmail());
        response.setPhone(seller.getPhone());
        response.setDescription(seller.getDescription());
        return response;
    }

    /**
     * Converts a MarketplaceItem entity to a MarketplaceItemResponse DTO.
     * Includes the associated seller information by converting the seller entity to a response DTO.
     */
    private MarketplaceItemResponse toItemResponse(MarketplaceItem item) {
        MarketplaceItemResponse response = new MarketplaceItemResponse();
        response.setId(item.getId());
        response.setItemName(item.getItemName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setType(item.getType());
        response.setCondition(item.getCondition());
        response.setStatus(item.getStatus());
        response.setImageUrl(item.getImageUrl());
        if (item.getSeller() != null) {
            response.setSeller(toSellerResponse(item.getSeller()));
        }
        return response;
    }
}
