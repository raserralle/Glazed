package com.nnpg.glazed.modules.main.ahsniper.util;

import java.util.regex.Pattern;

/**
 * Centralized configuration and constants for AH Sniper module.
 * Eliminates magic numbers and provides single source of truth for timing values.
 */
public class AHSniperConstants {
    
    // ===================== TIMING CONSTANTS =====================
    /** Ticks to wait before the first delay action */
    public static final int INITIAL_DELAY_TICKS = 1;
    
    /** Ticks to wait before buy action */
    public static final int BUY_DELAY_TICKS = 0;
    
    /** Ticks to wait before confirm action */
    public static final int CONFIRM_DELAY_TICKS = 1;
    
    /** Ticks to wait before navigation action */
    public static final int NAVIGATION_DELAY_TICKS = 0;
    
    /** Ticks to wait before selling action */
    public static final int SELL_DELAY_TICKS = 2;
    
    /** Maximum ticks to wait for purchase confirmation before timing out */
    public static final int MAX_PURCHASE_TIMEOUT_TICKS = 100;
    
    /** Maximum ticks without action before auto-disabling module (55 seconds at 20 ticks/sec) */
    public static final int MAX_STAGNANT_TICKS = 2200;
    
    /** Minimum ticks between inventory checks */
    public static final int MIN_INVENTORY_CHECK_TICKS = 10;
    
    /** Maximum ticks for inventory check */
    public static final int MAX_INVENTORY_CHECK_TICKS = 50;
    
    /** Delay for page refresh navigation */
    public static final int PAGE_REFRESH_DELAY_TICKS = 1;
    
    /** Delay for opening auction house */
    public static final int AUCTION_HOUSE_OPEN_DELAY_TICKS = 10;
    
    // ===================== SCREEN SLOTS & SIZES =====================
    /** Maximum auction house slots per screen (6 rows = 54 slots - 10 UI) */
    public static final int MAX_AUCTION_SLOTS = 44;
    
    /** Sort button slot (typically bottom-right area) */
    public static final int SORT_BUTTON_SLOT = 47;
    
    /** Refresh/next page button slot */
    public static final int REFRESH_BUTTON_SLOT = 49;
    
    /** Confirmation screen item display slot */
    public static final int CONFIRMATION_ITEM_SLOT = 13;
    
    /** Three-row auction central item slot */
    public static final int THREE_ROW_ITEM_SLOT = 13;
    
    /** Three-row auction buy button slot */
    public static final int THREE_ROW_BUY_SLOT = 15;
    
    // ===================== PRICE PARSING CONSTANTS =====================
    /** Price multiplier for billions (B suffix) */
    public static final double BILLION_MULTIPLIER = 1_000_000_000.0;
    
    /** Price multiplier for millions (M suffix) */
    public static final double MILLION_MULTIPLIER = 1_000_000.0;
    
    /** Price multiplier for thousands (K suffix) */
    public static final double THOUSAND_MULTIPLIER = 1_000.0;
    
    /** Error return value for price parsing */
    public static final double PARSE_ERROR = -1.0;
    
    // ===================== REGEX PATTERNS (Compiled Once) =====================
    public static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*d");
    public static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+)\\s*h");
    public static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+)\\s*m(?!in)");
    public static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*s");
    public static final Pattern PRICE_PATTERN = Pattern.compile("(?:\\$)?([\\d,.]+)\\s*([kmb])?", Pattern.CASE_INSENSITIVE);
    public static final Pattern ENCHANTMENT_LEVEL_PATTERN = Pattern.compile(".*(\\b(?:[IVX]+|\\d+))\\s*$");
    public static final Pattern SELLER_NAME_PATTERN = Pattern.compile("(?i)(?:seller|sold by|listed by)[:\\s]+(\\w+)");
    
    // ===================== WEBHOOK DEFAULTS =====================
    public static final long WEBHOOK_TIMEOUT_SECONDS = 15L;
    public static final String WEBHOOK_USERNAME = "Glazed AH Sniper";
    public static final String WEBHOOK_AVATAR_URL = "https://i.imgur.com/PHRZBjd.png";
    public static final String WEBHOOK_THUMBNAIL_URL = "https://i.imgur.com/PHRZBjd.png";
    
    // ===================== ITEM NAMES & ALIASES =====================
    /** Items to skip (shulker boxes are resale containers) */
    public static final String[] SHULKER_BOX_NAMES = {
        "SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX",
        "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX",
        "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX",
        "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX"
    };
    
    public static final String[] CURSED_ENCHANTMENTS = {"vanishing_curse", "binding_curse"};
    
    // ===================== CONFIRM BUTTON ITEMS =====================
    public static final String[] CONFIRM_BUTTON_NAMES = {"confirm", "buy", "yes", "accept"};
    public static final String[] CONFIRM_BUTTON_ITEMS = {
        "LIME_WOOL", "LIME_DYE", "GREEN_CONCRETE", "GREEN_CONCRETE_POWDER",
        "LIME_STAINED_GLASS", "EMERALD", "LIME_TERRACOTTA", "LIME_STAINED_GLASS_PANE"
    };
    
    // ===================== CANCEL BUTTON ITEMS =====================
    public static final String[] CANCEL_BUTTON_NAMES = {"cancel", "no", "decline", "reject"};
    public static final String[] CANCEL_BUTTON_ITEMS = {
        "RED_STAINED_GLASS", "RED_WOOL", "RED_DYE", "RED_CONCRETE",
        "RED_CONCRETE_POWDER", "RED_TERRACOTTA", "RED_STAINED_GLASS_PANE"
    };
    
    // ===================== ENCHANTMENT DETECTION =====================
    public static final String[] DETECTABLE_ENCHANTMENTS = {
        "Sharpness", "Protection", "Efficiency", "Fortune", "Silk Touch", "Unbreaking", "Mending",
        "Power", "Punch", "Flame", "Infinity", "Looting", "Knockback", "Fire Aspect",
        "Smite", "Bane of Arthropods", "Sweeping Edge", "Thorns", "Respiration", "Aqua Affinity",
        "Depth Strider", "Frost Walker", "Feather Falling", "Blast Protection", "Projectile Protection",
        "Fire Protection"
    };
    
    // Private constructor to prevent instantiation
    private AHSniperConstants() {
        throw new AssertionError("Utility class - do not instantiate");
    }
}
