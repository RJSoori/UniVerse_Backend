package com.example.backend_service.marketplace.enums;

/**
 * Tracks the current status of a marketplace item.
 * ACTIVE: Item is currently available for purchase or rent.
 * SOLD: Item has been purchased and is no longer available.
 * RENTED: Item is currently rented out.
 * REMOVED: Item listing has been deleted by the seller.
 */
public enum ItemStatus {
    ACTIVE,
    SOLD,
    RENTED,
    REMOVED
}