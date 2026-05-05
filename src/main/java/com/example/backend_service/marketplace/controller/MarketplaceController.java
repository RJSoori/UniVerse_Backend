package com.example.backend_service.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.model.Seller;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import com.example.backend_service.marketplace.repository.SellerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private MarketplaceItemRepository itemRepository;

    // --- Seller Endpoints ---
    @PostMapping("/sellers")
    public Seller registerSeller(@RequestBody Seller seller) {
        return sellerRepository.save(seller);
    }

    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Seller> getAllSellers() {
        return sellerRepository.findAll();
    }

    // --- Item Endpoints ---
    @PostMapping("/items")
    public MarketplaceItem addItem(@RequestBody MarketplaceItem item) {
        return itemRepository.save(item);
    }

    @GetMapping("/items")
    public List<MarketplaceItem> getAllItems() {
        return itemRepository.findAll();
    }
}
