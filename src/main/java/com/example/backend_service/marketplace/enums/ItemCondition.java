package com.example.backend_service.marketplace.enums;

/**
 * Describes the physical condition of a marketplace item.
 * BRAND_NEW: Item has never been used.
 * LIKE_NEW: Item is in excellent condition, appears unused.
 * GOOD: Item is in good working condition with minimal wear.
 * FAIR: Item shows signs of use but functions properly.
 * FOR_PARTS: Item is non-functional and being sold for parts only.
 */
public enum ItemCondition {
    BRAND_NEW,
    LIKE_NEW,
    GOOD,
    FAIR,
    FOR_PARTS
}