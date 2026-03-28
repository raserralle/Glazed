package com.nnpg.glazed.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Statistics {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STATS_FILE_NAME = "ah_sniper_stats.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // All-time statistics
    public double allTimeSpent = 0.0;
    public double allTimeSold = 0.0;

    // Daily statistics
    public double dailySpent = 0.0;
    public double dailySold = 0.0;
    public String lastResetDate = LocalDate.now().format(DATE_FORMATTER);

    // Purchase tracking
    public List<Transaction> purchaseHistory = new ArrayList<>();
    public int totalItemsPurchased = 0;
    public int dailyItemsPurchased = 0;
    public int attemptedPurchases = 0;

    // Sales tracking
    public List<Transaction> salesHistory = new ArrayList<>();
    public int totalItemsSold = 0;
    public int dailyItemsSold = 0;
    public int listedButUnsold = 0;

    // Performance metrics
    public int peakItemsOnSale = 0;
    public long totalSessionTime = 0; // milliseconds
    public int totalSessions = 0;

    // Inner class for transactions
    public static class Transaction {
        public long timestamp;
        public double price;
        public int quantity;
        public String type; // "BUY" or "SELL"

        public Transaction(long timestamp, double price, int quantity, String type) {
            this.timestamp = timestamp;
            this.price = price;
            this.quantity = quantity;
            this.type = type;
        }
    }

    /**
     * Load statistics from JSON file. If file doesn't exist, creates one with defaults.
     */
    public void load() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("glazed");
            Files.createDirectories(configDir);

            File statsFile = configDir.resolve(STATS_FILE_NAME).toFile();

            if (statsFile.exists()) {
                try (FileReader reader = new FileReader(statsFile)) {
                    Statistics loaded = GSON.fromJson(reader, Statistics.class);
                    if (loaded != null) {
                        this.allTimeSpent = loaded.allTimeSpent;
                        this.allTimeSold = loaded.allTimeSold;
                        this.dailySpent = loaded.dailySpent;
                        this.dailySold = loaded.dailySold;
                        this.lastResetDate = loaded.lastResetDate;
                        this.totalItemsPurchased = loaded.totalItemsPurchased;
                        this.dailyItemsPurchased = loaded.dailyItemsPurchased;
                        this.attemptedPurchases = loaded.attemptedPurchases;
                        this.totalItemsSold = loaded.totalItemsSold;
                        this.dailyItemsSold = loaded.dailyItemsSold;
                        this.listedButUnsold = loaded.listedButUnsold;
                        this.peakItemsOnSale = loaded.peakItemsOnSale;
                        this.totalSessionTime = loaded.totalSessionTime;
                        this.totalSessions = loaded.totalSessions;
                        this.purchaseHistory = loaded.purchaseHistory != null ? loaded.purchaseHistory : new ArrayList<>();
                        this.salesHistory = loaded.salesHistory != null ? loaded.salesHistory : new ArrayList<>();
                    }
                } catch (Exception e) {
                    System.err.println("[AHSniper Statistics] Failed to load stats: " + e.getMessage());
                }
            }

            // Check if daily reset is needed
            checkAndResetDaily();
        } catch (IOException e) {
            System.err.println("[AHSniper Statistics] Failed to create config directory: " + e.getMessage());
        }
    }

    /**
     * Save statistics to JSON file.
     */
    public void save() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("glazed");
            Files.createDirectories(configDir);

            File statsFile = configDir.resolve(STATS_FILE_NAME).toFile();

            try (FileWriter writer = new FileWriter(statsFile)) {
                GSON.toJson(this, writer);
            } catch (Exception e) {
                System.err.println("[AHSniper Statistics] Failed to save stats: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("[AHSniper Statistics] Failed to create config directory: " + e.getMessage());
        }
    }

    /**
     * Increment spent amount for both all-time and daily tracking.
     */
    public void incrementSpent(double amount) {
        checkAndResetDaily();
        this.allTimeSpent += amount;
        this.dailySpent += amount;
        this.save();
    }

    /**
     * Increment sold amount for both all-time and daily tracking.
     */
    public void incrementSold(double amount) {
        checkAndResetDaily();
        this.allTimeSold += amount;
        this.dailySold += amount;
        this.save();
    }

    /**
     * Get all-time profit (sold - spent).
     */
    public double getAllTimeProfit() {
        return this.allTimeSold - this.allTimeSpent;
    }

    /**
     * Get daily profit (sold - spent).
     */
    public double getDailyProfit() {
        return this.dailySold - this.dailySpent;
    }

    /**
     * Get hourly rate based on session duration.
     * @param sessionStartTime Session start time in milliseconds
     * @return Hourly profit rate
     */
    public double getHourlyRate(long sessionStartTime) {
        long sessionDurationMillis = System.currentTimeMillis() - sessionStartTime;
        double sessionDurationHours = sessionDurationMillis / 3600000.0;

        if (sessionDurationHours <= 0) {
            return 0.0;
        }

        return getDailyProfit() / sessionDurationHours;
    }

    /**
     * Check if day has changed since last reset, and reset daily counters if needed.
     */
    private void checkAndResetDaily() {
        String today = LocalDate.now().format(DATE_FORMATTER);

        if (!today.equals(this.lastResetDate)) {
            this.dailySpent = 0.0;
            this.dailySold = 0.0;
            this.dailyItemsPurchased = 0;
            this.dailyItemsSold = 0;
            this.lastResetDate = today;
            this.save();
        }
    }

    /**
     * Manually reset all statistics (all-time and daily).
     */
    public void resetAll() {
        this.allTimeSpent = 0.0;
        this.allTimeSold = 0.0;
        this.dailySpent = 0.0;
        this.dailySold = 0.0;
        this.totalItemsPurchased = 0;
        this.dailyItemsPurchased = 0;
        this.attemptedPurchases = 0;
        this.totalItemsSold = 0;
        this.dailyItemsSold = 0;
        this.listedButUnsold = 0;
        this.peakItemsOnSale = 0;
        this.totalSessionTime = 0;
        this.totalSessions = 0;
        this.purchaseHistory.clear();
        this.salesHistory.clear();
        this.lastResetDate = LocalDate.now().format(DATE_FORMATTER);
        this.save();
    }

    /**
     * Manually reset daily statistics only.
     */
    public void resetDaily() {
        this.dailySpent = 0.0;
        this.dailySold = 0.0;
        this.dailyItemsPurchased = 0;
        this.dailyItemsSold = 0;
        this.lastResetDate = LocalDate.now().format(DATE_FORMATTER);
        this.save();
    }

    /**
     * Record a purchase transaction.
     */
    public void recordPurchase(double price, int quantity) {
        checkAndResetDaily();
        this.purchaseHistory.add(new Transaction(System.currentTimeMillis(), price, quantity, "BUY"));
        this.totalItemsPurchased += quantity;
        this.dailyItemsPurchased += quantity;
        this.save();
    }

    /**
     * Record a sale transaction.
     */
    public void recordSale(double price, int quantity) {
        checkAndResetDaily();
        this.salesHistory.add(new Transaction(System.currentTimeMillis(), price, quantity, "SELL"));
        this.totalItemsSold += quantity;
        this.dailyItemsSold += quantity;
        this.save();
    }

    /**
     * Track attempted purchase.
     */
    public void recordAttemptedPurchase() {
        this.attemptedPurchases++;
        this.save();
    }

    /**
     * Update peak items on sale.
     */
    public void updatePeakItemsOnSale(int current) {
        if (current > this.peakItemsOnSale) {
            this.peakItemsOnSale = current;
            this.save();
        }
    }

    /**
     * Add items that were never sold.
     */
    public void addUnsoldsItems(int count) {
        this.listedButUnsold += count;
        this.save();
    }

    /**
     * Add session time.
     */
    public void addSessionTime(long sessionDuration) {
        this.totalSessionTime += sessionDuration;
        this.totalSessions++;
        this.save();
    }

    /**
     * Get average purchase price (all-time).
     */
    public double getAvgPurchasePrice() {
        if (this.totalItemsPurchased <= 0) return 0.0;
        return this.allTimeSpent / this.totalItemsPurchased;
    }

    /**
     * Get average sale price (all-time).
     */
    public double getAvgSalePrice() {
        if (this.totalItemsSold <= 0) return 0.0;
        return this.allTimeSold / this.totalItemsSold;
    }

    /**
     * Get average profit per transaction (all-time).
     */
    public double getAvgProfitPerTransaction() {
        int totalTransactions = this.totalItemsPurchased + this.totalItemsSold;
        if (totalTransactions <= 0) return 0.0;
        return this.getAllTimeProfit() / totalTransactions;
    }

    /**
     * Calculate profit margin as percentage (all-time).
     */
    public double calculateProfitMarginPercent() {
        if (this.allTimeSpent <= 0) return 0.0;
        return (this.getAllTimeProfit() / this.allTimeSpent) * 100.0;
    }

    /**
     * Calculate daily profit margin as percentage.
     */
    public double calculateDailyProfitMarginPercent() {
        if (this.dailySpent <= 0) return 0.0;
        return (this.getDailyProfit() / this.dailySpent) * 100.0;
    }

    /**
     * Calculate ROI (Return on Investment) as percentage.
     */
    public double calculateROI() {
        if (this.allTimeSpent <= 0) return 0.0;
        return (this.getAllTimeProfit() / this.allTimeSpent) * 100.0;
    }

    /**
     * Calculate items per hour (all-time average).
     */
    public double calculateItemsPerHour() {
        if (this.totalSessionTime <= 0) return 0.0;
        double hours = this.totalSessionTime / 3600000.0;
        return this.totalItemsSold / hours;
    }

    /**
     * Calculate purchase success rate as percentage.
     */
    public double calculatePurchaseSuccessRate() {
        int total = this.totalItemsPurchased + this.attemptedPurchases;
        if (total <= 0) return 0.0;
        return ((double) this.totalItemsPurchased / total) * 100.0;
    }

    /**
     * Calculate sale success rate as percentage.
     */
    public double calculateSaleSuccessRate() {
        int total = this.totalItemsSold + this.listedButUnsold;
        if (total <= 0) return 0.0;
        return ((double) this.totalItemsSold / total) * 100.0;
    }

    /**
     * Get average time to sell (milliseconds).
     */
    public double getAverageTimeToSell() {
        if (this.totalSessions <= 0) return 0.0;
        return this.totalSessionTime / (double) this.totalSessions;
    }

    /**
     * Build comprehensive detailed stats object.
     * @param currentItemsOnSale Current number of items on sale
     * @param sessionDurationMs Current session duration in milliseconds
     * @return DetailedStats object with all calculated metrics
     */
    public DetailedStats getDetailedStats(int currentItemsOnSale, long sessionDurationMs) {
        DetailedStats stats = new DetailedStats();

        // Purchase stats
        stats.totalItemsPurchased = this.totalItemsPurchased;
        stats.totalSpent = this.allTimeSpent;
        stats.dailySpent = this.dailySpent;
        stats.avgPurchasePrice = this.getAvgPurchasePrice();

        // Sales stats
        stats.totalItemsSold = this.totalItemsSold;
        stats.totalSold = this.allTimeSold;
        stats.dailySold = this.dailySold;
        stats.avgSalePrice = this.getAvgSalePrice();

        // Profit metrics
        stats.allTimeProfit = this.getAllTimeProfit();
        stats.dailyProfit = this.getDailyProfit();
        stats.sessionProfit = this.getDailyProfit(); // Session is same as current daily stats
        stats.profitMarginPercent = this.calculateProfitMarginPercent();
        stats.dailyProfitMarginPercent = this.calculateDailyProfitMarginPercent();
        stats.avgProfitPerTransaction = this.getAvgProfitPerTransaction();
        stats.roi = this.calculateROI();

        // Time metrics
        stats.hourlyRate = this.getHourlyRate(System.currentTimeMillis() - sessionDurationMs);
        stats.itemsPerHour = this.calculateItemsPerHour();
        stats.totalSessionTimeMs = this.totalSessionTime;
        stats.avgTimeToSellMs = this.getAverageTimeToSell();

        // Inventory
        stats.currentItemsOnSale = currentItemsOnSale;
        stats.peakItemsOnSale = this.peakItemsOnSale;
        stats.listedButUnsold = this.listedButUnsold;

        // Performance
        stats.purchaseSuccessRate = this.calculatePurchaseSuccessRate();
        stats.saleSuccessRate = this.calculateSaleSuccessRate();

        // Metadata
        stats.totalSessions = this.totalSessions;

        return stats;
    }
}
