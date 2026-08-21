package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.ItemCondition;
import com.example.backend_service.marketplace.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for marketplace item creation requests.
 * Contains item details including name, description, price, condition, and type with validation constraints.
 */
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

    private String category;

    // How many identical units this listing represents. Optional; defaults to 1.
    private Integer totalUnits;

    @NotNull
    private Long sellerId;

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
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getTotalUnits() { return totalUnits; }
    public void setTotalUnits(Integer totalUnits) { this.totalUnits = totalUnits; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}