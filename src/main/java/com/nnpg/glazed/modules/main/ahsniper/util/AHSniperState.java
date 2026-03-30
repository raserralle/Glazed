package com.nnpg.glazed.modules.main.ahsniper.util;

/**
 * State machine for AH Sniper module lifecycle.
 * Replaces 40+ boolean flags with a single source of truth.
 */
public enum AHSniperState {
    IDLE("Idle", "Waiting for activation or ready state"),
    SEARCHING("Searching", "Looking through auction house items"),
    FOUND_ITEM("Found Item", "Located a valid item to purchase"),
    WAITING_BUY("Waiting Buy", "Pending buy button click"),
    CONFIRMING("Confirming", "In confirmation screen, pending confirmation"),
    WAITING_CONFIRM("Waiting Confirm", "Waiting for confirm delay to elapse"),
    PURCHASED("Purchased", "Item purchase confirmed, checking inventory"),
    SELLING("Selling", "Item acquired, listing on auction house"),
    COMPLETED("Completed", "Purchase and sale cycle complete");

    private final String displayName;
    private final String description;

    AHSniperState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this state allows searching for new items.
     */
    public boolean canSearch() {
        return this == IDLE || this == SEARCHING;
    }

    /**
     * Check if this state represents an active purchase flow.
     */
    public boolean isPurchasing() {
        return this == FOUND_ITEM || this == WAITING_BUY || 
               this == CONFIRMING || this == WAITING_CONFIRM || this == PURCHASED;
    }

    /**
     * Check if purchase flow has completed.
     */
    public boolean isPurchaseComplete() {
        return this == COMPLETED || this == SELLING;
    }
}
