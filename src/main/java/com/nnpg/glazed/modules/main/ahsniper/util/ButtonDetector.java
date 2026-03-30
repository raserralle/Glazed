package com.nnpg.glazed.modules.main.ahsniper.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Utility class for detecting UI buttons in container screens.
 * Consolidates button detection logic with improved maintainability.
 */
public class ButtonDetector {
    
    private ButtonDetector() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Check if an item stack represents a confirm button.
     * 
     * @param stack The item stack to check
     * @return true if this appears to be a confirm button
     */
    public static boolean isConfirmButton(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check by name first (fallback for custom servers)
        String name = stack.getName().getString().toLowerCase();
        for (String confirmName : AHSniperConstants.CONFIRM_BUTTON_NAMES) {
            if (name.contains(confirmName)) {
                return true;
            }
        }

        // Check by item type (primary method)
        return isConfirmButtonItem(stack.getItem());
    }

    /**
     * Check if an item is a confirm button by type.
     * Uses Item enum comparison for reliability.
     */
    private static boolean isConfirmButtonItem(Item item) {
        return item == Items.LIME_WOOL || item == Items.LIME_DYE ||
               item == Items.GREEN_CONCRETE || item == Items.GREEN_CONCRETE_POWDER ||
               item == Items.LIME_STAINED_GLASS || item == Items.EMERALD ||
               item == Items.LIME_TERRACOTTA || item == Items.LIME_STAINED_GLASS_PANE;
    }

    /**
     * Check if an item stack represents a cancel button.
     * 
     * @param stack The item stack to check
     * @return true if this appears to be a cancel button
     */
    public static boolean isCancelButton(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check by name first (fallback for custom servers)
        String name = stack.getName().getString().toLowerCase();
        for (String cancelName : AHSniperConstants.CANCEL_BUTTON_NAMES) {
            if (name.contains(cancelName)) {
                return true;
            }
        }

        // Check by item type (primary method)
        return isCancelButtonItem(stack.getItem());
    }

    /**
     * Check if an item is a cancel button by type.
     * Uses Item enum comparison for reliability.
     */
    private static boolean isCancelButtonItem(Item item) {
        return item == Items.RED_STAINED_GLASS || item == Items.RED_WOOL || 
               item == Items.RED_DYE || item == Items.RED_CONCRETE ||
               item == Items.RED_CONCRETE_POWDER || item == Items.RED_TERRACOTTA || 
               item == Items.RED_STAINED_GLASS_PANE;
    }

    /**
     * Check if item is a sort/filter button (usually bottom-right).
     * Should contain "Sort", "Filter", or "Recently Listed" in name.
     */
    public static boolean isSortButton(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String name = stack.getName().getString().toLowerCase();
        return name.contains("sort") || name.contains("filter") || 
               name.contains("recently listed");
    }

    /**
     * Check if item is a refresh/pagination button.
     * Should contain "refresh", "next", "page", etc.
     */
    public static boolean isRefreshButton(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String name = stack.getName().getString().toLowerCase();
        return name.contains("refresh") || name.contains("next") || 
               name.contains("page") || name.contains("→");
    }
}
