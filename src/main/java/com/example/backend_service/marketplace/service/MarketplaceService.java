package com.example.backend_service.marketplace.service;

import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
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

    // --- Seller Auth Methods ---
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

    public SellerAuthResponse loginSeller(SellerLoginRequest request) {
        Seller seller = sellerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), seller.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        String token = sellerJwtService.issue(seller.getId());
        return new SellerAuthResponse(token, toSellerResponse(seller));
    }

    public SellerResponse getSellerByToken(String token) {
        Long sellerId = sellerJwtService.parse(token);
        return getSellerById(sellerId);
    }

    // --- Seller Methods ---
    public List<SellerResponse> getAllSellers() {
        return sellerRepository.findAll()
                .stream()
                .map(this::toSellerResponse)
                .collect(Collectors.toList());
    }

    public SellerResponse getSellerById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + id));
        return toSellerResponse(seller);
    }

    // --- Item Methods ---
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

    public List<MarketplaceItemResponse> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    public List<MarketplaceItemResponse> getItemsBySeller(Long sellerId) {
        return itemRepository.findBySellerId(sellerId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    public MarketplaceItemResponse getItemById(Long id) {
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        return toItemResponse(item);
    }

    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    // --- Mapping Methods ---
    private SellerResponse toSellerResponse(Seller seller) {
        SellerResponse response = new SellerResponse();
        response.setId(seller.getId());
        response.setStoreName(seller.getStoreName());
        response.setEmail(seller.getEmail());
        response.setPhone(seller.getPhone());
        response.setDescription(seller.getDescription());
        return response;
    }

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