package com.example.backend_service.marketplace.controller;

import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    @Autowired
    private MarketplaceService marketplaceService;

    // --- Item Endpoints ---
    @GetMapping("/items")
    public List<MarketplaceItemResponse> getAllItems() {
        return marketplaceService.getAllItems();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<MarketplaceItemResponse> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getItemById(id));
    }

    @PostMapping("/items")
    public ResponseEntity<MarketplaceItemResponse> createItem(@RequestBody MarketplaceItemRequest request) {
        return ResponseEntity.ok(marketplaceService.createItem(request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        marketplaceService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // --- Seller Endpoints ---
    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SellerResponse> getAllSellers() {
        return marketplaceService.getAllSellers();
    }

    @GetMapping("/sellers/{id}")
    public ResponseEntity<SellerResponse> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getSellerById(id));
    }

    @PostMapping("/sellers")
    public ResponseEntity<SellerResponse> createSeller(@RequestBody SellerRequest request) {
        return ResponseEntity.ok(marketplaceService.createSeller(request));
    }
}