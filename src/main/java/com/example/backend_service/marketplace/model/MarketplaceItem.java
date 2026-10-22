package com.example.backend_service.marketplace.model;

import com.example.backend_service.marketplace.enums.ItemCondition;
import com.example.backend_service.marketplace.enums.ItemStatus;
import com.example.backend_service.marketplace.enums.ItemType;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item listed in the marketplace.
 * Stores item details including name, description, price, condition, and type (sell or rent).
 * Each item is associated with a seller and tracks its current status (active, sold, rented, or removed).
 */
@Entity
@Table(name = "marketplace_items")
public class MarketplaceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.ACTIVE;

    // Remembers the status this item had right before its seller was banned (ACTIVE,
    // SOLD, or RENTED), so lifting the ban can restore it correctly instead of just
    // resetting everything to ACTIVE. Null except while status == SELLER_BANNED.
    @Enumerated(EnumType.STRING)
    private ItemStatus previousStatus;

    // Legacy single-image field, kept as the "primary" photo for backward compatibility
    // with anywhere the app just needs one thumbnail. Always mirrors imageUrls.get(0).
    private String imageUrl;

    // Full photo set for the listing (2-8 photos, enforced by the seller-facing upload
    // flow — not by the database). Ordered by upload order.
    @ElementCollection
    @CollectionTable(name = "marketplace_item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "image_url", nullable = false, length = 1000)
    @OrderColumn(name = "display_order")
    private List<String> imageUrls = new ArrayList<>();

    // Free-text category (e.g. "Textbooks & Notes"), matching a fixed list of choices
    // on the frontend. Stored as a plain string rather than a native enum column so the
    // category list can change without a schema migration.
    private String category;

    // How many times this item's detail view has been fetched. Drives "Trending Items".
    // Explicit DEFAULT so adding this column to the existing (non-empty) table doesn't
    // trip MySQL strict mode's "no default value" rejection.
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private long viewCount = 0;

    // How many identical units this listing represents, and how many of those have been
    // sold/rented out so far (recorded by the seller — there's no in-app checkout, so
    // this can't be tracked automatically). Available units = totalUnits - soldUnits;
    // once that hits zero the item's status flips to SOLD or RENTED.
    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private int totalUnits = 1;
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int soldUnits = 0;

    // Cumulative count of times this listing has been rented out. Unlike soldUnits (which
    // resets to 0 when reopenForRent makes the listing available again), this never resets
    // — re-renting the same listing counts as a new rental for reporting purposes.
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int timesRented = 0;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;

    public MarketplaceItem() {}

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
    public ItemStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ItemStatus previousStatus) { this.previousStatus = previousStatus; }
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
    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
}