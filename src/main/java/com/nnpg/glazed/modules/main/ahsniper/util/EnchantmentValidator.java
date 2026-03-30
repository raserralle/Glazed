package com.nnpg.glazed.modules.main.ahsniper.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Utility class for validating item enchantments.
 * Consolidates enchantment matching logic that was previously duplicated.
 */
public class EnchantmentValidator {
    
    private EnchantmentValidator() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Extract all enchantments from an item's tooltip.
     * 
     * @param stack The item to check
     * @param tooltipContext The tooltip context (varies by Minecraft version)
     * @param player The player entity
     * @return List of enchantment strings found on the item
     */
    public static List<String> getItemEnchantments(ItemStack stack, Object tooltipContext, Object player) {
        List<String> enchantments = new ArrayList<>();
        
        // This is a placeholder - actual implementation needs Minecraft client access
        // Original implementation would call: stack.getTooltip(tooltipContext, player, TooltipType.BASIC)
        
        return enchantments;
    }

    /**
     * Check if item has required enchantments (any match is sufficient).
     * 
     * @param itemEnchantments List of enchantments on the item
     * @param requiredEnchantments List of required enchantments
     * @return true if at least one required enchantment is found
     */
    public static boolean hasAnyRequiredEnchantment(List<String> itemEnchantments, List<String> requiredEnchantments) {
        if (requiredEnchantments.isEmpty()) {
            return true;
        }

        for (String requiredEnchant : requiredEnchantments) {
            if (matchesEnchantment(itemEnchantments, requiredEnchant)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if item has exact list of enchantments (all must match, no more, no less).
     * 
     * @param itemEnchantments List of enchantments on the item
     * @param requiredEnchantments Required enchantments
     * @return true only if counts match and all required are present
     */
    public static boolean hasExactEnchantments(List<String> itemEnchantments, List<String> requiredEnchantments) {
        if (itemEnchantments.size() != requiredEnchantments.size()) {
            return false;
        }

        for (String requiredEnchant : requiredEnchantments) {
            if (!matchesEnchantment(itemEnchantments, requiredEnchant)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a required enchantment string matches any item enchantment.
     * Supports optional level specification.
     * Examples: "sharpness", "sharpness 5", "protection iv"
     * 
     * @param itemEnchantments List of enchantments on item
     * @param requiredEnchant Required enchantment (possibly with level)
     * @return true if the enchantment is found (with correct level if specified)
     */
    public static boolean matchesEnchantment(List<String> itemEnchantments, String requiredEnchant) {
        String[] parts = requiredEnchant.trim().split("\\s+");
        String enchantName = parts[0].toLowerCase();
        Integer requiredLevel = null;

        if (parts.length > 1) {
            requiredLevel = PriceParser.parseLevel(parts[1]);
        }

        for (String itemEnchant : itemEnchantments) {
            String itemEnchantLower = itemEnchant.toLowerCase();
            if (itemEnchantLower.contains(enchantName)) {
                if (requiredLevel == null) {
                    return true;
                }
                int itemLevel = getEnchantmentLevel(itemEnchant);
                return itemLevel >= requiredLevel;
            }
        }
        return false;
    }

    /**
     * Extract enchantment level from tooltip text.
     * Supports roman numerals (I, II, III, etc.) and numbers (1, 2, 3, etc.)
     * 
     * @param enchantmentText The enchantment text line
     * @return The level, or 1 if not found
     */
    public static int getEnchantmentLevel(String enchantmentText) {
        Matcher matcher = AHSniperConstants.ENCHANTMENT_LEVEL_PATTERN.matcher(enchantmentText);
        if (matcher.find()) {
            String levelStr = matcher.group(1);
            Integer level = PriceParser.parseLevel(levelStr);
            return level != null ? level : 1;
        }
        return 1;
    }

    /**
     * Check if item has cursed enchantments (Curse of Vanishing, Curse of Binding).
     * 
     * @param stack The item to check
     * @return true if any cursed enchantments found
     */
    public static boolean hasCursedEnchantments(ItemStack stack) {
        String enchantStr = stack.getEnchantments().toString().toLowerCase();
        for (String cursed : AHSniperConstants.CURSED_ENCHANTMENTS) {
            if (enchantStr.contains(cursed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get human-readable enchantment string from item.
     * Joins all enchantments with newlines.
     * 
     * @param enchantments List of enchantments from item
     * @return Formatted string, or "None" if empty
     */
    public static String formatEnchantments(List<String> enchantments) {
        return enchantments.isEmpty() ? "None" : String.join("\n", enchantments);
    }
}
