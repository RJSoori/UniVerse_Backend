package com.example.backend_service.marketplace.controller;

import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.dto.SellerUpdateRequest;
import com.example.backend_service.marketplace.service.MarketplaceService;
import com.example.backend_service.marketplace.service.SellerJwtService;
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

    @Autowired
    private SellerJwtService sellerJwtService;

    /**
     * Allows a new seller to register by creating an account with store information.
     * Returns JWT token and seller profile upon successful registration.
     */
    @PostMapping("/sellers/register")
    public ResponseEntity<SellerAuthResponse> registerSeller(@RequestBody SellerRequest request) {
        return ResponseEntity.ok(marketplaceService.registerSeller(request));
    }

    /** 
     * Authenticates a seller by verifying username and password.
     * Issues a JWT token for subsequent authenticated requests.
     */
    @PostMapping("/sellers/login")
    public ResponseEntity<SellerAuthResponse> loginSeller(@RequestBody SellerLoginRequest request) {
        return ResponseEntity.ok(marketplaceService.loginSeller(request));
    }

    /**
     * Retrieves the authenticated seller's own profile information using the X-Seller-Token header.
     * Validates token presence before processing the request.
     */
    @GetMapping("/sellers/me")
    public ResponseEntity<SellerResponse> getMySellerProfile(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken) {
        if (sellerToken == null) {
            return ResponseEntity.status(401).build();
        }
        Long sellerId = sellerJwtService.parse(sellerToken);
        return ResponseEntity.ok(marketplaceService.getSellerById(sellerId));
    }

    /**
     * Retrieves all registered sellers in the system. Restricted to admin users only.
     */
    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SellerResponse> getAllSellers() {
        return marketplaceService.getAllSellers();
    }

    /**
     * Retrieves a specific seller's public profile information by seller ID.
     * Available to all users without authentication.
     */
    @GetMapping("/sellers/{id}")
    public ResponseEntity<SellerResponse> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getSellerById(id));
    }
    
    @PutMapping("/sellers/me")
    public ResponseEntity<SellerResponse> updateMySellerProfile(
        @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
        @RequestBody SellerUpdateRequest request) {
    if (sellerToken == null) {
        return ResponseEntity.status(401).build();
    }
    Long sellerId = sellerJwtService.parse(sellerToken);
    return ResponseEntity.ok(marketplaceService.updateSeller(sellerId, request));
    }
    
    /**
     * Retrieves all marketplace items currently listed.
     * Available to all users for browsing purposes.
     */
    @GetMapping("/items")
    public List<MarketplaceItemResponse> getAllItems() {
        return marketplaceService.getAllItems();
    }

    /**
     * Retrieves details of a specific marketplace item by its ID.
     */
    @GetMapping("/items/{id}")
    public ResponseEntity<MarketplaceItemResponse> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getItemById(id));
    }

    /**
     * Retrieves all items listed by a specific seller.
     */
    @GetMapping("/items/seller/{sellerId}")
    public List<MarketplaceItemResponse> getItemsBySeller(@PathVariable Long sellerId) {
        return marketplaceService.getItemsBySeller(sellerId);
    }

    /**
     * Allows an authenticated seller to list a new item in the marketplace.
     * Automatically assigns the item to the authenticated seller based on JWT token.
     */
    @PostMapping("/items")
    public ResponseEntity<MarketplaceItemResponse> createItem(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @RequestBody MarketplaceItemRequest request) {
        if (sellerToken != null) {
            Long sellerId = sellerJwtService.parse(sellerToken);
            request.setSellerId(sellerId);
        }
        return ResponseEntity.ok(marketplaceService.createItem(request));
    }

    /**
     * Allows an authenticated seller to remove their marketplace item listing.
     * Item is permanently deleted from the system.
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @PathVariable Long id) {
        marketplaceService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}