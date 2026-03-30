package com.nnpg.glazed.modules.main.ahsniper.util;

import com.nnpg.glazed.utils.DebugLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Handles all Discord webhook communications for the AH Sniper module.
 * Encapsulates webhook creation, sending, and error handling.
 */
public class DiscordWebhook {
    
    private final String webhookUrl;
    private final boolean enabled;
    private final boolean debugMode;
    private final boolean selfPing;
    private final String discordId;
    private final HttpClient httpClient;

    public DiscordWebhook(String webhookUrl, boolean enabled, boolean debugMode, 
                         boolean selfPing, String discordId, HttpClient httpClient) {
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
        this.debugMode = debugMode;
        this.selfPing = selfPing;
        this.discordId = discordId;
        this.httpClient = httpClient;
    }

    /**
     * Check if webhook is properly configured and enabled.
     */
    public boolean canSend() {
        return enabled && webhookUrl != null && !webhookUrl.isEmpty();
    }

    /**
     * Send a purchase success notification to Discord.
     */
    public void sendPurchaseSuccess(String itemName, double actualPrice, int quantity, 
                                    String enchantments, String destructionTimer, 
                                    String playerName, int itemsOnSale, String snipeMode, 
                                    double maxPrice, String priceMode, double defaultMaxPrice) {
        if (!canSend()) {
            return;
        }

        String jsonPayload = createPurchaseSuccessEmbed(
            itemName, actualPrice, quantity, enchantments, destructionTimer,
            playerName, itemsOnSale, snipeMode, maxPrice, priceMode, defaultMaxPrice
        );
        
        sendMessage(jsonPayload, "Purchase Success");
    }

    /**
     * Send an auto-sell notification to Discord.
     */
    public void sendAutoSell(String itemName, double salePrice, String playerName,
                            double allTimeSpent, double allTimeSold, double dailySpent,
                            double dailySold, long sessionStartTime, int itemsOnSale) {
        if (!canSend()) {
            return;
        }

        String jsonPayload = createAutoSellEmbed(
            itemName, salePrice, playerName, allTimeSpent, allTimeSold,
            dailySpent, dailySold, sessionStartTime, itemsOnSale
        );
        
        sendMessage(jsonPayload, "Auto-Sell");
    }

    /**
     * Send a test webhook message.
     */
    public void sendTest(String playerName) {
        if (!canSend()) {
            return;
        }

        String testPayload = createTestMessage(playerName);
        sendMessage(testPayload, "Test");
    }

    // ===================== PRIVATE METHODS (Message Creation) =====================

    private String createPurchaseSuccessEmbed(String itemName, double actualPrice, int quantity,
                                             String enchantments, String destructionTimer,
                                             String playerName, int itemsOnSale, String snipeMode,
                                             double maxPrice, String priceMode, double defaultMaxPrice) {
        long timestamp = System.currentTimeMillis() / 1000L;

        String pingContent = getPingContent();
        String actualPriceStr = PriceParser.formatPrice(actualPrice);
        String maxPriceStr = PriceParser.formatPrice(maxPrice);
        double savings = maxPrice - actualPrice;
        String savingsStr = PriceParser.formatPrice(Math.abs(savings));
        String savingsPercentage = String.format("%.1f%%", savings / maxPrice * 100.0);

        String messageContent = String.format("%s🧣 **%s** sniped **%dx %s** for **%s**!",
            pingContent, playerName, quantity, itemName, actualPriceStr);
        String description = String.format("💰 **Savings** of %s (**%s**)", savingsStr, savingsPercentage);

        String enchantValue = enchantments.equals("None") || enchantments.isEmpty() 
            ? "None" 
            : enchantments.trim();

        return String.format(
            "{\"content\":\"%s\",\"username\":\"%s\",\"avatar_url\":\"%s\",\"embeds\":[{" +
            "\"title\":\"Glazed AH Sniper Alert [%s]\",\"description\":\"%s\",\"color\":8388736," +
            "\"thumbnail\":{\"url\":\"%s\"},\"fields\":[" +
            "{\"name\":\"📦 Item\",\"value\":\"%s x%d\",\"inline\":true}," +
            "{\"name\":\"💰 Purchase Price\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"💵 Max Price\",\"value\":\"%s (%s)\",\"inline\":true}," +
            "{\"name\":\"✨ Enchantments\",\"value\":\"%s\",\"inline\":false}," +
            "{\"name\":\"⏱️ Destruction Timer\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"📊 Items On Sale\",\"value\":\"%s\",\"inline\":true}" +
            "],\"footer\":{\"text\":\"Glazed AH Sniper V2\"},\"timestamp\":\"%s\"}]}",
            escapeJson(messageContent), escapeJson(AHSniperConstants.WEBHOOK_USERNAME), 
            escapeJson(AHSniperConstants.WEBHOOK_AVATAR_URL),
            snipeMode, escapeJson(description), escapeJson(AHSniperConstants.WEBHOOK_THUMBNAIL_URL),
            escapeJson(itemName), quantity, escapeJson(actualPriceStr),
            escapeJson(maxPriceStr), escapeJson(priceMode.toLowerCase()),
            escapeJson(enchantValue), escapeJson(destructionTimer), itemsOnSale, Instant.now().toString()
        );
    }

    private String createAutoSellEmbed(String itemName, double salePrice, String playerName,
                                      double allTimeSpent, double allTimeSold, double dailySpent,
                                      double dailySold, long sessionStartTime, int itemsOnSale) {
        long timestamp = System.currentTimeMillis() / 1000L;

        String pingContent = getPingContent();
        String salePriceStr = PriceParser.formatPrice(salePrice);
        String allTimeSpentStr = PriceParser.formatPrice(allTimeSpent);
        String allTimeSoldStr = PriceParser.formatPrice(allTimeSold);
        String dailySpentStr = PriceParser.formatPrice(dailySpent);
        String dailySoldStr = PriceParser.formatPrice(dailySold);
        
        double allTimeProfit = allTimeSold - allTimeSpent;
        double dailyProfit = dailySold - dailySpent;
        double hourlyRate = calculateHourlyRate(allTimeSold - allTimeSpent, sessionStartTime);
        
        String allTimeProfitStr = PriceParser.formatPrice(Math.abs(allTimeProfit));
        String dailyProfitStr = PriceParser.formatPrice(Math.abs(dailyProfit));
        String hourlyRateStr = PriceParser.formatPrice(Math.abs(hourlyRate));
        
        String allTimeProfitStatus = allTimeProfit >= 0 ? "✅ All-Time Profit" : "❌ All-Time Loss";
        String dailyProfitStatus = dailyProfit >= 0 ? "✅ Daily Profit" : "❌ Daily Loss";

        String messageContent = String.format("%s💰 **%s** sold **%s** for **%s**!",
            pingContent, playerName, itemName, salePriceStr);

        return String.format(
            "{\"content\":\"%s\",\"username\":\"%s\",\"avatar_url\":\"%s\",\"embeds\":[{" +
            "\"title\":\"Glazed AH Sniper AutoSell Alert\",\"description\":\"Item sold through auto-sell. Comprehensive stats enabled!\",\"color\":65280," +
            "\"thumbnail\":{\"url\":\"%s\"},\"fields\":[" +
            "{\"name\":\"📦 Item\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"💵 Sale Price\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"⏰ Timestamp\",\"value\":\"<t:%d:R>\",\"inline\":true}," +
            "{\"name\":\"💸 All-Time Spent\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"💰 All-Time Sold\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"%s\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"💸 Daily Spent\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"💰 Daily Sold\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"%s\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"⚡ Hourly Rate\",\"value\":\"%s/hr\",\"inline\":true}," +
            "{\"name\":\"📊 Items On Sale\",\"value\":\"%s\",\"inline\":false}" +
            "],\"footer\":{\"text\":\"Glazed AH Sniper V2\"},\"timestamp\":\"%s\"}]}",
            escapeJson(messageContent), escapeJson(AHSniperConstants.WEBHOOK_USERNAME),
            escapeJson(AHSniperConstants.WEBHOOK_AVATAR_URL),
            escapeJson(AHSniperConstants.WEBHOOK_THUMBNAIL_URL),
            escapeJson(itemName), escapeJson(salePriceStr), timestamp,
            escapeJson(allTimeSpentStr), escapeJson(allTimeSoldStr), allTimeProfitStatus, 
            escapeJson(allTimeProfitStr),
            escapeJson(dailySpentStr), escapeJson(dailySoldStr), dailyProfitStatus, 
            escapeJson(dailyProfitStr),
            escapeJson(hourlyRateStr), itemsOnSale, Instant.now().toString()
        );
    }

    private String createTestMessage(String playerName) {
        return String.format(
            "{\"content\":\"Webhook Test - AH Sniper is working for **%s**!\",\"username\":\"%s\"}",
            escapeJson(playerName), escapeJson(AHSniperConstants.WEBHOOK_USERNAME)
        );
    }

    // ===================== PRIVATE METHODS (HTTP & UTILITIES) =====================

    private void sendMessage(String jsonPayload, String messageType) {
        try {
            if (debugMode) {
                DebugLogger.logWebhookDebug(String.format(
                    "Sending %s webhook request. Payload preview: %s",
                    messageType, jsonPayload.substring(0, Math.min(jsonPayload.length(), 200))
                ));
            }

            long timestamp = System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .header("User-Agent", "AH-Sniper/1.0")
                .timeout(Duration.ofSeconds(AHSniperConstants.WEBHOOK_TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - timestamp;

            if (response.statusCode() != 204 && response.statusCode() != 200) {
                String error = String.format("%s webhook failed - Status: %d", messageType, response.statusCode());
                DebugLogger.logWebhookError(error);
                if (debugMode) {
                    DebugLogger.logWebhookDebug("Response body: " + response.body());
                }
            } else if (debugMode) {
                DebugLogger.logWebhookDebug(String.format("%s webhook sent successfully - Status: %d", 
                    messageType, response.statusCode()));
            }
        } catch (Exception e) {
            String error = String.format("%s webhook error: %s", messageType, e.getMessage());
            DebugLogger.logWebhookError(error);
            e.printStackTrace();
        }
    }

    private String getPingContent() {
        if (selfPing && discordId != null && !discordId.trim().isEmpty()) {
            return String.format("<@%s> ", discordId.trim());
        }
        return "";
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private double calculateHourlyRate(double profit, long sessionStartTime) {
        if (sessionStartTime <= 0) return 0;
        long elapsedMs = System.currentTimeMillis() - sessionStartTime;
        if (elapsedMs <= 0) return 0;
        double hours = elapsedMs / 3600000.0;
        return profit / hours;
    }
}
