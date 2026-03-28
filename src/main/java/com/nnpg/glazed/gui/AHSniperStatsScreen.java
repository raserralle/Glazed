package com.nnpg.glazed.gui;

import com.nnpg.glazed.modules.main.AHSniper;
import com.nnpg.glazed.utils.DetailedStats;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * GUI screen for displaying AH Sniper statistics.
 */
public class AHSniperStatsScreen extends WindowScreen {
    private final AHSniper module;
    private DetailedStats currentStats;
    private WVerticalList statsContainer;

    public AHSniperStatsScreen(GuiTheme theme, AHSniper module) {
        super(theme, "AH Sniper - Statistics");
        this.module = module;
    }

    @Override
    public void initWidgets() {
        // Refresh stats
        long sessionDuration = System.currentTimeMillis() - this.module.getSessionStartTime();
        this.currentStats = this.module.getStats().getDetailedStats(this.module.getItemsOnSale(), sessionDuration);

        buildUI();
    }

    private void buildUI() {
        // Title
        addMessage("📊 AH Sniper Detailed Statistics");
        add(this.theme.horizontalSeparator()).padVertical(this.theme.scale(4)).expandX();

        // Stats container
        statsContainer = add(this.theme.verticalList()).expandX().widget();

        // Purchase Statistics Section
        addStatsSection("💳 PURCHASE STATISTICS", 
            "Total Items: " + this.currentStats.totalItemsPurchased,
            "Total Spent: " + formatPrice(this.currentStats.totalSpent),
            "Daily Spent: " + formatPrice(this.currentStats.dailySpent),
            "Avg Price: " + formatPrice(this.currentStats.avgPurchasePrice)
        );

        // Sales Statistics Section
        addStatsSection("💰 SALES STATISTICS",
            "Total Items: " + this.currentStats.totalItemsSold,
            "Total Revenue: " + formatPrice(this.currentStats.totalSold),
            "Daily Revenue: " + formatPrice(this.currentStats.dailySold),
            "Avg Price: " + formatPrice(this.currentStats.avgSalePrice)
        );

        // Profit Metrics Section
        String profitStatus = this.currentStats.allTimeProfit >= 0 ? "✅" : "❌";
        addStatsSection("📈 PROFIT METRICS",
            profitStatus + " All-Time: " + formatPrice(this.currentStats.allTimeProfit) + " (" + String.format("%.1f%%", this.currentStats.profitMarginPercent) + ")",
            "Daily Profit: " + formatPrice(this.currentStats.dailyProfit) + " (" + String.format("%.1f%%", this.currentStats.dailyProfitMarginPercent) + ")",
            "Session Profit: " + formatPrice(this.currentStats.sessionProfit),
            "Avg Per Transaction: " + formatPrice(this.currentStats.avgProfitPerTransaction),
            "ROI: " + String.format("%.2f%%", this.currentStats.roi)
        );

        // Performance Metrics Section
        addStatsSection("⚡ PERFORMANCE METRICS",
            "Items/Hour: " + String.format("%.2f", this.currentStats.itemsPerHour),
            "Hourly Rate: " + formatPrice(this.currentStats.hourlyRate) + "/hr",
            "Purchase Success: " + String.format("%.1f%%", this.currentStats.purchaseSuccessRate),
            "Sale Success: " + String.format("%.1f%%", this.currentStats.saleSuccessRate),
            "Avg Time to Sell: " + formatMillis((long)this.currentStats.avgTimeToSellMs)
        );

        // Inventory Status Section
        addStatsSection("📦 INVENTORY STATUS",
            "Current Items: " + this.currentStats.currentItemsOnSale,
            "Peak Items: " + this.currentStats.peakItemsOnSale,
            "Never Sold: " + this.currentStats.listedButUnsold
        );

        // Session Info Section
        addStatsSection("📊 SESSION INFORMATION",
            "Total Sessions: " + this.currentStats.totalSessions,
            "Session Time: " + formatMillis(this.currentStats.totalSessionTimeMs),
            "Generated: " + java.time.Instant.ofEpochMilli(this.currentStats.generatedAt).toString()
        );

        add(this.theme.horizontalSeparator()).padVertical(this.theme.scale(8)).expandX();

        // Predictive: Trends
        addStatsSection("📈 TREND ANALYSIS",
            "Profit Acceleration: " + String.format("%.2f/hr", this.currentStats.profitAcceleration),
            "Success Rate Trend: " + String.format("%.1f%%", this.currentStats.successRateTrend),
            "Price Volatility: " + formatPrice(this.currentStats.priceVolatility)
        );

        // Predictive: Projections
        addStatsSection("🔮 PROJECTIONS",
            "Projected Daily: " + formatPrice(this.currentStats.projectedDailyProfit),
            "Items Clear In: " + String.format("%.1f hours", this.currentStats.inventoryTurnoverHours),
            "Avg Time To Sell: " + formatMillis((long)this.currentStats.avgTimeToSellMs)
        );

        // Predictive: Patterns
        addStatsSection("🎯 PATTERNS & PRICING",
            "Peak Hour: " + (this.currentStats.peakEarningHour >= 0 ? this.currentStats.peakEarningHour + ":00" : "N/A"),
            "Best Snipe Hour: " + (this.currentStats.bestSnipeHour >= 0 ? this.currentStats.bestSnipeHour + ":00" : "N/A"),
            "Price Floor: " + formatPrice(this.currentStats.priceFloorEstimate),
            "Price Ceiling: " + formatPrice(this.currentStats.priceCeilingEstimate)
        );

        // Predictive: Efficiency
        addStatsSection("⚙️ EFFICIENCY METRICS",
            "Capital Efficiency: " + String.format("%.2f", this.currentStats.capitalEfficiency),
            "Capital Velocity: " + String.format("%.2f", this.currentStats.capitalVelocity),
            "Consistency: " + String.format("%.1f%%", this.currentStats.consistencyScore)
        );

        add(this.theme.horizontalSeparator()).padVertical(this.theme.scale(8)).expandX();

        // Button Container
        WHorizontalList buttonsContainer = add(this.theme.horizontalList()).expandX().widget();

        WButton sendWebhookButton = buttonsContainer.add(this.theme.button("📡 Send Webhook")).expandX().widget();
        sendWebhookButton.action = () -> {
            this.module.sendDetailedStatsWebhook();
            addNotification("Webhook sent!");
        };

        WButton refreshButton = buttonsContainer.add(this.theme.button("🔄 Refresh")).expandX().widget();
        refreshButton.action = this::refresh;

        WButton closeButton = buttonsContainer.add(this.theme.button("Close")).expandX().widget();
        closeButton.action = this::close;
    }

    private void addStatsSection(String title, String... stats) {
        // Title
        WHorizontalList titleContainer = statsContainer.add(this.theme.horizontalList()).expandX().widget();
        titleContainer.add(this.theme.label(title)).expandX();

        // Stats
        for (String stat : stats) {
            WHorizontalList statLine = statsContainer.add(this.theme.horizontalList()).expandX().widget();
            statLine.add(this.theme.label("  " + stat)).expandX();
        }

        // Separator
        statsContainer.add(this.theme.horizontalSeparator()).padVertical(this.theme.scale(2)).expandX();
    }

    private void addMessage(String message) {
        WHorizontalList l = add(this.theme.horizontalList()).expandX().widget();
        l.add(this.theme.label(message)).expandX();
    }

    private void addNotification(String message) {
        // Simple notification - just refresh the screen to show updated stats
        refresh();
    }

    private String formatPrice(double value) {
        if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format("%.2fK", value / 1_000);
        } else {
            return String.format("%.0f", value);
        }
    }

    private String formatMillis(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    private void refresh() {
        this.clear();
        initWidgets();
    }
}
