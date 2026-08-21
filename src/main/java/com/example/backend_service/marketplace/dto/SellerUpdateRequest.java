package com.example.backend_service.marketplace.dto;

public class SellerUpdateRequest {

    private String storeName;
    private String phone;
    private String description;

    // Notification preferences — null means "leave unchanged", matching the same
    // partial-update semantics as the fields above.
    private Boolean notifyNewMessage;
    private Boolean notifyNewOffer;
    private Boolean notifyListingExpiry;
    private Boolean notifyPlatformUpdates;

    // Getters and Setters
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getNotifyNewMessage() { return notifyNewMessage; }
    public void setNotifyNewMessage(Boolean notifyNewMessage) { this.notifyNewMessage = notifyNewMessage; }
    public Boolean getNotifyNewOffer() { return notifyNewOffer; }
    public void setNotifyNewOffer(Boolean notifyNewOffer) { this.notifyNewOffer = notifyNewOffer; }
    public Boolean getNotifyListingExpiry() { return notifyListingExpiry; }
    public void setNotifyListingExpiry(Boolean notifyListingExpiry) { this.notifyListingExpiry = notifyListingExpiry; }
    public Boolean getNotifyPlatformUpdates() { return notifyPlatformUpdates; }
    public void setNotifyPlatformUpdates(Boolean notifyPlatformUpdates) { this.notifyPlatformUpdates = notifyPlatformUpdates; }
}