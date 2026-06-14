package com.example.backend_service.marketplace.controller;

import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.dto.SellerUpdateRequest;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import com.example.backend_service.marketplace.service.MarketplaceService;
import com.example.backend_service.marketplace.service.SellerJwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private MarketplaceItemRepository itemRepository;

    @PostMapping("/sellers/register")
    public ResponseEntity<SellerAuthResponse> registerSeller(@RequestBody SellerRequest request) {
        return ResponseEntity.ok(marketplaceService.registerSeller(request));
    }

    @PostMapping("/sellers/login")
    public ResponseEntity<SellerAuthResponse> loginSeller(@RequestBody SellerLoginRequest request) {
        return ResponseEntity.ok(marketplaceService.loginSeller(request));
    }

    @GetMapping("/sellers/me")
    public ResponseEntity<SellerResponse> getMySellerProfile(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken) {
        if (sellerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long sellerId = sellerJwtService.parse(sellerToken);
        return ResponseEntity.ok(marketplaceService.getSellerById(sellerId));
    }

    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SellerResponse> getAllSellers() {
        return marketplaceService.getAllSellers();
    }

    @GetMapping("/sellers/{id}")
    public ResponseEntity<SellerResponse> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getSellerById(id));
    }

    @PutMapping("/sellers/me")
    public ResponseEntity<SellerResponse> updateMySellerProfile(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @RequestBody SellerUpdateRequest request) {
        if (sellerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long sellerId = sellerJwtService.parse(sellerToken);
        return ResponseEntity.ok(marketplaceService.updateSeller(sellerId, request));
    }

    @GetMapping("/items")
    public List<MarketplaceItemResponse> getAllItems() {
        return marketplaceService.getAllItems();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<MarketplaceItemResponse> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getItemById(id));
    }

    @GetMapping("/items/seller/{sellerId}")
    public List<MarketplaceItemResponse> getItemsBySeller(@PathVariable Long sellerId) {
        return marketplaceService.getItemsBySeller(sellerId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceItemResponse createItem(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @RequestBody MarketplaceItemRequest request) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        request.setSellerId(sellerId);
        return marketplaceService.createItem(request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @PathVariable Long id) {
        Long authSellerId = sellerJwtService.parse(sellerToken);
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        if (!authSellerId.equals(item.getSeller().getId())) {
            throw new ForbiddenException();
        }
        marketplaceService.deleteItem(id);
    }
}
