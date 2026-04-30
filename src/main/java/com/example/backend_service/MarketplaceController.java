package com.example.backend_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@CrossOrigin(origins = "*") // Allows your React frontend to connect
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
