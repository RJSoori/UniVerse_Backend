package com.example.backend_service.marketplace.enums;

/**
 * Tracks the current status of a marketplace item.
 * PENDING_APPROVAL: Listed by an unverified seller; awaiting admin moderation, not publicly visible.
 * ACTIVE: Item is currently available for purchase or rent.
 * REJECTED: Admin declined the listing; not publicly visible.
 * SOLD: Item has been purchased and is no longer available.
 * RENTED: Item is currently rented out.
 * REMOVED: Item listing has been deleted by the seller.
 * SELLER_BANNED: Item was ACTIVE but its seller was banned for repeated policy violations;
 * hidden from public browsing until an admin lifts the ban, at which point it reverts to ACTIVE.
 */
public enum ItemStatus {
    PENDING_APPROVAL,
    ACTIVE,
    REJECTED,
    SOLD,
    RENTED,
    REMOVED,
    SELLER_BANNED
}