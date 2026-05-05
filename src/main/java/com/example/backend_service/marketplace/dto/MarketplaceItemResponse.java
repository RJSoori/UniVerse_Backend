package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.ItemCondition;
import com.example.backend_service.marketplace.enums.ItemStatus;
import com.example.backend_service.marketplace.enums.ItemType;

public class MarketplaceItemResponse {

    private Long id;
    private String itemName;
    private String description;
    private Double price;
    private ItemType type;
    private ItemCondition condition;
    private ItemStatus status;
    private String imageUrl;
    private SellerResponse seller;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public ItemCondition getCondition() { return condition; }
    public void setCondition(ItemCondition condition) { this.condition = condition; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public SellerResponse getSeller() { return seller; }
    public void setSeller(SellerResponse seller) { this.seller = seller; }
}