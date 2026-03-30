package com.nnpg.glazed.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
    private static final File DEBUG_DIR = new File("./glazed_debug");
    private static final File DEBUG_LOG_FILE = new File(DEBUG_DIR, "ah_sniper_debug.txt");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    static {
        if (!DEBUG_DIR.exists()) {
            DEBUG_DIR.mkdirs();
        }
    }
    
    /**
     * Logs a general message to the debug log file
     */
    public static void log(String message) {
        try {
            FileWriter writer = new FileWriter(DEBUG_LOG_FILE, true);
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            writer.write("[" + timestamp + "] " + message + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Logs a timer check result
     * @param itemName Name of the item checked
     * @param timerHours Destruction timer in hours
     * @param minRequired Minimum required hours
     * @param passed Whether the timer check passed
     */
    public static void logTimerCheck(String itemName, double timerHours, double minRequired, boolean passed) {
        String status = passed ? "PASS" : "FAIL";
        String message = String.format("TIMER CHECK [%s] - Item: %s | Timer: %.2f hours | Min Required: %.2f hours | Result: %s",
            status, itemName, timerHours, minRequired, status);
        log(message);
    }
    
    /**
     * Logs item validation details
     * @param itemName Name of the item
     * @param price Price of the item
     * @param timerHours Destruction timer in hours
     * @param isValid Whether the item is valid
     * @param reason Why it passed or failed validation
     */
    public static void logItemValidation(String itemName, double price, double timerHours, boolean isValid, String reason) {
        String status = isValid ? "VALID" : "INVALID";
        String message = String.format("ITEM VALIDATION [%s] - Item: %s | Price: %.2f | Timer: %.2f hours | Reason: %s",
            status, itemName, price, timerHours, reason);
        log(message);
    }
    
    /**
     * Logs a purchase attempt
     * @param itemName Name of the item
     * @param quantity Quantity being purchased
     * @param actualPrice Actual price paid
     * @param timerHours Destruction timer in hours
     * @param timerStr Formatted timer string
     */
    public static void logPurchaseAttempt(String itemName, int quantity, double actualPrice, double timerHours, String timerStr) {
        String message = String.format("PURCHASE ATTEMPT - Item: %s | Qty: %d | Price: %.2f | Timer: %.2f hours (%s)",
            itemName, quantity, actualPrice, timerHours, timerStr);
        log(message);
    }
    
    /**
     * Logs a confirmed purchase
     * @param timerHours Destruction timer in hours
     * @param minRequired Minimum required hours
     */
    public static void logPurchaseConfirmed(double timerHours, double minRequired) {
        String message = String.format("PURCHASE CONFIRMED - Timer at confirmation: %.2f hours (min required: %.2f hours)",
            timerHours, minRequired);
        log(message);
    }
    
    /**
     * Logs a cancelled purchase
     * @param reason Reason for cancellation
     * @param timerHours Destruction timer in hours
     * @param minRequired Minimum required hours
     */
    public static void logPurchaseCancelled(String reason, double timerHours, double minRequired) {
        String message = String.format("PURCHASE CANCELLED - Reason: %s | Timer: %.2f hours (min required: %.2f hours)",
            reason, timerHours, minRequired);
        log(message);
    }
    
    /**
     * Logs current filter settings
     * @param filterLowTime Whether low time filtering is enabled
     * @param minHours Minimum hours required
     */
    public static void logFilterSettings(boolean filterLowTime, double minHours) {
        String message = String.format("FILTER SETTINGS - Filter Low Time: %s | Min Hours: %.2f",
            filterLowTime, minHours);
        log(message);
    }
    
    /**
     * Logs session start
     */
    public static void logSessionStart() {
        log("========== AH SNIPER SESSION STARTED ==========");
        log("Time: " + LocalDateTime.now());
    }
    
    /**
     * Logs session end
     */
    public static void logSessionEnd() {
        log("========== AH SNIPER SESSION ENDED ==========");
        log("Time: " + LocalDateTime.now());
    }
    
    /**
     * Gets the debug log file path
     */
    public static File getLogFile() {
        return DEBUG_LOG_FILE;
    }
}
