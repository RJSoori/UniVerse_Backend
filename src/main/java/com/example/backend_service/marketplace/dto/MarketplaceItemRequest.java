package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.ItemCondition;
import com.example.backend_service.marketplace.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MarketplaceItemRequest {

    @NotBlank
    private String itemName;

    private String description;

    @NotNull
    private Double price;

    @NotNull
    private ItemType type;

    @NotNull
    private ItemCondition condition;

    private String imageUrl;

    @NotNull
    private Long sellerId;

    // Getters and Setters
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}