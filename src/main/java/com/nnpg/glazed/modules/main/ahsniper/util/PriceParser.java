package com.nnpg.glazed.modules.main.ahsniper.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing prices, times, and other numeric values from item tooltips and settings.
 * Centralizes all parsing logic to reduce code duplication and improve maintainability.
 */
public class PriceParser {
    
    private PriceParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Parse a price string with K/M/B suffixes.
     * Examples: "100", "1k", "2.5m", "1b"
     * 
     * @param priceStr The price string to parse
     * @return The parsed price value, or -1.0 if parsing fails
     */
    public static double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return AHSniperConstants.PARSE_ERROR;
        }

        String cleaned = priceStr.trim().toLowerCase().replace(",", "");
        double multiplier = 1.0;

        if (cleaned.endsWith("b")) {
            multiplier = AHSniperConstants.BILLION_MULTIPLIER;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier = AHSniperConstants.MILLION_MULTIPLIER;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("k")) {
            multiplier = AHSniperConstants.THOUSAND_MULTIPLIER;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        try {
            return Double.parseDouble(cleaned) * multiplier;
        } catch (NumberFormatException e) {
            return AHSniperConstants.PARSE_ERROR;
        }
    }

    /**
     * Parse a destruction timer from item tooltip.
     * Looks for lines like "Self Destruct: 5d 12h 30m"
     * 
     * @param stack The item stack to check
     * @param tooltipContext The tooltip context
     * @param player The player (used in tooltip creation)
     * @return Hours remaining until destruction, or -1.0 if not found
     */
    public static double parseSelfDestructTime(ItemStack stack, Item.TooltipContext tooltipContext, Object player) {
        if (stack == null || stack.isEmpty()) {
            return AHSniperConstants.PARSE_ERROR;
        }

        // Extract tooltip - player parameter not used in this Minecraft version
        List<Text> tooltip = stack.getTooltip(tooltipContext, null, TooltipType.BASIC);

        for (int i = 0; i < tooltip.size(); i++) {
            String line = tooltip.get(i).getString().toLowerCase();

            // Detect the "Self Destruct" header
            if (line.contains("self destruct")) {
                // Timer is always on the next line
                if (i + 1 >= tooltip.size()) return AHSniperConstants.PARSE_ERROR;

                String timer = tooltip.get(i + 1).getString().toLowerCase();

                double hours = 0.0;

                Matcher d = Pattern.compile("(\\d+)\\s*d").matcher(timer);
                if (d.find()) hours += Integer.parseInt(d.group(1)) * 24.0;

                Matcher h = Pattern.compile("(\\d+)\\s*h").matcher(timer);
                if (h.find()) hours += Integer.parseInt(h.group(1));

                Matcher m = Pattern.compile("(\\d+)\\s*m(?!in)").matcher(timer);
                if (m.find()) hours += Integer.parseInt(m.group(1)) / 60.0;

                Matcher s = Pattern.compile("(\\d+)\\s*s").matcher(timer);
                if (s.find()) hours += Integer.parseInt(s.group(1)) / 3600.0;

                return hours;
            }
        }

        return AHSniperConstants.PARSE_ERROR;
    }

    /**
     * Parse time string with days, hours, minutes, seconds.
     * Examples: "5d 12h", "48h 30m", "2d"
     * 
     * @param timeStr The time string to parse
     * @return Total hours, or -1.0 if parsing fails
     */
    public static double parseTimeString(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return AHSniperConstants.PARSE_ERROR;
        }

        double totalHours = 0.0;
        timeStr = timeStr.toLowerCase();
        boolean foundComponent = false;

        Matcher days = Pattern.compile("(\\d+)\\s*d").matcher(timeStr);
        if (days.find()) {
            totalHours += Double.parseDouble(days.group(1)) * 24.0;
            foundComponent = true;
        }

        Matcher hrs = Pattern.compile("(\\d+)\\s*h").matcher(timeStr);
        if (hrs.find()) {
            totalHours += Double.parseDouble(hrs.group(1));
            foundComponent = true;
        }

        return foundComponent ? totalHours : AHSniperConstants.PARSE_ERROR;
    }

    /**
     * Parse price from tooltip lines.
     * Looks for lines with $ or price: prefix
     * 
     * @param tooltip The tooltip lines to search
     * @return The parsed price, or -1.0 if not found
     */
    public static double parseTooltipPrice(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            return AHSniperConstants.PARSE_ERROR;
        }

        for (Text line : tooltip) {
            String text = line.getString().toLowerCase().replace(",", "");

            // Accept lines that look like price lines
            if (!(text.startsWith("$") || text.contains("price:"))) {
                continue;
            }

            Matcher matcher = AHSniperConstants.PRICE_PATTERN.matcher(text);
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                String suffix = matcher.group(2) != null ? matcher.group(2).toLowerCase() : "";

                try {
                    double base = Double.parseDouble(numberStr);
                    return switch (suffix) {
                        case "k" -> base * AHSniperConstants.THOUSAND_MULTIPLIER;
                        case "m" -> base * AHSniperConstants.MILLION_MULTIPLIER;
                        case "b" -> base * AHSniperConstants.BILLION_MULTIPLIER;
                        default -> base;
                    };
                } catch (NumberFormatException ignored) {
                    // Continue searching
                }
            }
        }

        return AHSniperConstants.PARSE_ERROR;
    }

    /**
     * Format a double price value into human-readable format.
     * Examples: 1000000 -> "1.0M", 500 -> "500"
     * 
     * @param price The price to format
     * @return Formatted price string
     */
    public static String formatPrice(double price) {
        if (price >= AHSniperConstants.BILLION_MULTIPLIER) {
            return String.format("%.1fB", price / AHSniperConstants.BILLION_MULTIPLIER);
        } else if (price >= AHSniperConstants.MILLION_MULTIPLIER) {
            return String.format("%.1fM", price / AHSniperConstants.MILLION_MULTIPLIER);
        } else if (price >= AHSniperConstants.THOUSAND_MULTIPLIER) {
            return String.format("%.1fK", price / AHSniperConstants.THOUSAND_MULTIPLIER);
        } else {
            return String.format("%.0f", price);
        }
    }

    /**
     * Format destruction timer in human-readable format.
     * Examples: 48.5 hours -> "2d", 12.5 hours -> "12.5h"
     * 
     * @param hours Hours remaining
     * @return Formatted timer string, or "N/A" if invalid
     */
    public static String formatDestructionTimer(double hours) {
        if (hours < 0 || hours == 0) {
            return "N/A";
        }

        if (hours >= 24) {
            int days = (int) hours / 24;
            int remainingHours = (int) hours % 24;
            if (remainingHours > 0) {
                return String.format("%dd %dh", days, remainingHours);
            } else {
                return String.format("%dd", days);
            }
        } else {
            return String.format("%.1fh", hours);
        }
    }

    /**
     * Parse integer level from text, supporting roman numerals or numbers.
     * Examples: "IV" -> 4, "5" -> 5, "V" -> 5
     * 
     * @param levelStr The level string to parse
     * @return The parsed level, or null if invalid
     */
    public static Integer parseLevel(String levelStr) {
        if (levelStr == null || levelStr.isEmpty()) {
            return null;
        }

        levelStr = levelStr.trim().toUpperCase();

        // Try roman numeral first
        Integer roman = parseRomanNumeral(levelStr);
        if (roman != null) {
            return roman;
        }

        // Try regular number
        try {
            return Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a roman numeral string to integer.
     * 
     * @param roman The roman numeral (e.g., "V", "IV")
     * @return The numeric value, or null if invalid
     */
    public static Integer parseRomanNumeral(String roman) {
        return switch (roman.toUpperCase()) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            default -> null;
        };
    }
}
