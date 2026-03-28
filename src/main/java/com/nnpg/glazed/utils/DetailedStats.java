package com.nnpg.glazed.utils;

/**
 * Container for all detailed statistics metrics.
 * Used for displaying comprehensive stats in GUI and webhooks.
 */
public class DetailedStats {
    // Purchase stats
    public int totalItemsPurchased;
    public double totalSpent;
    public double dailySpent;
    public double avgPurchasePrice;

    // Sales stats
    public int totalItemsSold;
    public double totalSold;
    public double dailySold;
    public double avgSalePrice;

    // Profit metrics
    public double allTimeProfit;
    public double dailyProfit;
    public double sessionProfit;
    public double profitMarginPercent;
    public double dailyProfitMarginPercent;
    public double avgProfitPerTransaction;
    public double roi;

    // Time metrics
    public double hourlyRate;
    public double itemsPerHour;
    public long totalSessionTimeMs;
    public double avgTimeToSellMs;

    // Inventory
    public int currentItemsOnSale;
    public int peakItemsOnSale;
    public int listedButUnsold;

    // Performance
    public double purchaseSuccessRate;
    public double saleSuccessRate;

    // Predictive: Trends
    public double profitAcceleration; // Change in hourly rate over time
    public double successRateTrend; // Improvement/decline in success rate
    public double priceVolatility; // Average deviation from mean price

    // Predictive: Projections
    public double projectedDailyProfit; // Extrapolated profit if current rate continues
    public double inventoryTurnoverHours; // Hours until current items likely sold

    // Predictive: Patterns
    public int peakEarningHour; // Which hour of day has most transactions
    public int bestSnipeHour; // Which hour has highest success rate
    public double priceFloorEstimate; // Estimated minimum profitable buy price
    public double priceCeilingEstimate; // Estimated maximum sale price

    // Predictive: Efficiency
    public double capitalEfficiency; // Profit per coin invested
    public double capitalVelocity; // How fast money turns over (sales/spent ratio)
    public double consistencyScore; // Variance in profits (higher = more consistent)

    // Metadata
    public int totalSessions;
    public long generatedAt;

    public DetailedStats() {
        this.generatedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "DetailedStats{" +
                "\n  Purchase Stats: items=" + totalItemsPurchased + ", spent=" + totalSpent + ", avg=" + avgPurchasePrice +
                "\n  Sales Stats: items=" + totalItemsSold + ", sold=" + totalSold + ", avg=" + avgSalePrice +
                "\n  Profit: allTime=" + allTimeProfit + ", daily=" + dailyProfit + ", margin=" + profitMarginPercent + "%" +
                "\n  Performance: success=" + purchaseSuccessRate + "%, saleSuccess=" + saleSuccessRate + "%" +
                "\n  Inventory: current=" + currentItemsOnSale + ", peak=" + peakItemsOnSale + ", unsold=" + listedButUnsold +
                "\n}";
    }
}
