package com.example.backend_service.marketplace.dto;

import com.example.backend_service.marketplace.enums.ItemCondition;
import com.example.backend_service.marketplace.enums.ItemStatus;
import com.example.backend_service.marketplace.enums.ItemType;

import java.util.List;

/**
 * Data transfer object for marketplace item information in API responses.
 * Contains complete item details including condition, status, type, and associated seller information.
 * Returned when retrieving item listings or after item creation/updates.
 */
public class MarketplaceItemResponse {

    private Long id;
    private String itemName;
    private String description;
    private Double price;
    private ItemType type;
    private ItemCondition condition;
    private ItemStatus status;
    private String imageUrl;
    private List<String> imageUrls;
    private String category;
    private long viewCount;
    private int totalUnits;
    private int soldUnits;
    private int timesRented;
    private SellerResponse seller;

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
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
    public int getSoldUnits() { return soldUnits; }
    public void setSoldUnits(int soldUnits) { this.soldUnits = soldUnits; }
    public int getTimesRented() { return timesRented; }
    public void setTimesRented(int timesRented) { this.timesRented = timesRented; }
    public SellerResponse getSeller() { return seller; }
    public void setSeller(SellerResponse seller) { this.seller = seller; }
}