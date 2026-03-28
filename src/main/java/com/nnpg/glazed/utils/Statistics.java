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
        this.lastResetDate = LocalDate.now().format(DATE_FORMATTER);
        this.save();
    }

    /**
     * Manually reset daily statistics only.
     */
    public void resetDaily() {
        this.dailySpent = 0.0;
        this.dailySold = 0.0;
        this.lastResetDate = LocalDate.now().format(DATE_FORMATTER);
        this.save();
    }
}
