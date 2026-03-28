package com.nnpg.glazed.modules.main;

import com.nnpg.glazed.GlazedAddon;
import com.nnpg.glazed.VersionUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class AHSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWebhook = settings.createGroup("Discord Webhook");

    private final Setting<String> sellPrice = sgGeneral.add(new StringSetting.Builder()
        .name("sell-price")
        .description("The price to list each hotbar item for. Supports K/M/B.")
        .defaultValue("30k")
        .build()
    );

    private final Setting<Integer> confirmDelay = sgGeneral.add(new IntSetting.Builder()
        .name("confirm-delay")
        .description("Delay in ticks before clicking the confirm button.")
        .defaultValue(10)
        .min(0)
        .max(100)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show chat notifications.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> webhookEnabled = sgWebhook.add(new BoolSetting.Builder()
        .name("webhook-enabled")
        .description("Enable Discord webhook notifications for sold items.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> webhookUrl = sgWebhook.add(new StringSetting.Builder()
        .name("webhook-url")
        .description("Discord webhook URL.")
        .defaultValue("")
        .visible(this.webhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> selfPing = sgWebhook.add(new BoolSetting.Builder()
        .name("self-ping")
        .description("Ping yourself in the webhook message.")
        .defaultValue(false)
        .visible(this.webhookEnabled::get)
        .build()
    );

    private final Setting<String> discordId = sgWebhook.add(new StringSetting.Builder()
        .name("discord-id")
        .description("Your Discord user ID for pinging.")
        .defaultValue("")
        .visible(() -> this.webhookEnabled.get() && this.selfPing.get())
        .build()
    );

    private final Setting<Boolean> debugMode = sgWebhook.add(new BoolSetting.Builder()
        .name("debug-mode")
        .description("Enable debug logging for webhook issues.")
        .defaultValue(false)
        .visible(this.webhookEnabled::get)
        .build()
    );

    private final Setting<Boolean> enableFilter = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-item-filter")
        .description("Only sell selected item type from the hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Item> filterItem = sgGeneral.add(new ItemSetting.Builder()
        .name("filter-item")
        .description("Only this item will be sold when filter is enabled.")
        .defaultValue(Items.DIAMOND)
        .build()
    );

    private int delayCounter = 0;
    private boolean awaitingConfirmation = false;
    private int currentSlot = 0;
    private String lastSoldItemName = "";
    private double lastSoldPrice = 0;
    private final HttpClient httpClient;

    public AHSell() {
        super(GlazedAddon.CATEGORY, "ah-sell", "Automatically sells all hotbar items using /ah sell.");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
    }

    @Override
    public void onActivate() {
        if (!isValidPrice(sellPrice.get())) {
            if (notifications.get()) error("Invalid price format: " + sellPrice.get());
            toggle();
            return;
        }

        if (!hasSellableItemsInHotbar()) {
            if (notifications.get()) error("No sellable items found in hotbar.");
            toggle();
            return;
        }

        currentSlot = 0;
        attemptSellCurrentSlot();
    }

    @Override
    public void onDeactivate() {
        awaitingConfirmation = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!awaitingConfirmation || mc.player == null) return;

        if (delayCounter > 0) {
            delayCounter--;
            return;
        }

        ScreenHandler screenHandler = mc.player.currentScreenHandler;

        if (screenHandler instanceof GenericContainerScreenHandler handler) {
            if (handler.getRows() == 3) {
                ItemStack confirmButton = handler.getSlot(15).getStack();
                if (!confirmButton.isEmpty()) {
                    mc.interactionManager.clickSlot(handler.syncId, 15, 1, SlotActionType.QUICK_MOVE, mc.player);
                    if (notifications.get()) info("Sold item in hotbar slot " + currentSlot + ".");
                }

                awaitingConfirmation = false;
                moveToNextSlot();
            }
        }
    }

    @EventHandler
    private void onChatMessage(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();
        if (msg.contains("You have too many listed items.")) {
            if (notifications.get()) warning("Sell limit reached! Disabling module.");
            toggle();
        }

        // Detect sale message: "PlayerName bought your ItemName for $Price"
        if (msg.contains("bought your")) {
            // Item was sold - trigger webhook
            if (this.webhookEnabled.get()) {
                this.sendSaleWebhook(this.lastSoldItemName, this.lastSoldPrice);
            }
            if (this.notifications.get()) {
                info("Item sold! %s for %s", this.lastSoldItemName, formatPrice(this.lastSoldPrice));
            }
        }
    }

    private void attemptSellCurrentSlot() {
        if (currentSlot > 8) {
            if (notifications.get()) info("Finished processing hotbar. Disabling module.");
            toggle();
            return;
        }

        // Use VersionUtil to handle version differences
        VersionUtil.setSelectedSlot(mc.player, currentSlot);
        ItemStack stack = mc.player.getInventory().getStack(currentSlot);

        if (enableFilter.get() && (stack.isEmpty() || !stack.isOf(filterItem.get()))) {
            if (notifications.get()) info("Skipping slot " + currentSlot + " (does not match filter).");
            moveToNextSlot();
            return;
        }

        if (stack.isEmpty()) {
            moveToNextSlot();
            return;
        }

        String price = sellPrice.get().trim();
        double parsedPrice = parsePrice(price);

        if (parsedPrice <= 0) {
            if (notifications.get()) error("Invalid price format: " + price);
            toggle();
            return;
        }

        if (notifications.get()) {
            info("Sending /ah sell %s for slot %d", formatPrice(parsedPrice), currentSlot);
        }

        // Store item name and price for webhook
        this.lastSoldItemName = stack.getName().getString();
        this.lastSoldPrice = parsedPrice;

        mc.getNetworkHandler().sendChatCommand("ah sell " + price);
        delayCounter = confirmDelay.get();
        awaitingConfirmation = true;
    }

    private void moveToNextSlot() {
        currentSlot++;
        attemptSellCurrentSlot();
    }

    private boolean hasSellableItemsInHotbar() {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;

            if (enableFilter.get()) {
                if (stack.isOf(filterItem.get())) return true;
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPrice(String priceStr) {
        return parsePrice(priceStr) > 0;
    }

    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return -1.0;

        String cleaned = priceStr.trim().toUpperCase();
        double multiplier = 1.0;

        if (cleaned.endsWith("B")) {
            multiplier = 1_000_000_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("M")) {
            multiplier = 1_000_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("K")) {
            multiplier = 1_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        try {
            return Double.parseDouble(cleaned) * multiplier;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) {
            return String.format("%.2fB", price / 1_000_000_000);
        } else if (price >= 1_000_000) {
            return String.format("%.2fM", price / 1_000_000);
        } else if (price >= 1_000) {
            return String.format("%.2fK", price / 1_000);
        } else {
            return String.format("%.2f", price);
        }
    }

    private void sendSaleWebhook(String itemName, double salePrice) {
        if (!this.webhookEnabled.get() || this.webhookUrl.get().isEmpty()) {
            if (this.debugMode.get()) {
                info("Debug: Webhook disabled or no URL set");
            }
            return;
        }

        if (this.debugMode.get()) {
            info("Debug: Sending sale webhook for %s at %s", itemName, formatPrice(salePrice));
        }

        String jsonPayload = this.createSaleEmbed(itemName, salePrice);
        this.sendWebhookMessage(jsonPayload, "Sale");
    }

    private void sendWebhookMessage(String jsonPayload, String messageType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.webhookUrl.get()))
                .header("Content-Type", "application/json")
                .header("User-Agent", "AH-Sell/1.0")
                .timeout(Duration.ofSeconds(15L))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 200) {
                if (this.debugMode.get() || this.notifications.get()) {
                    ChatUtils.error("%s webhook failed - Status: %d", messageType, response.statusCode());
                }
                if (this.debugMode.get()) {
                    info("Debug: Response body: %s", response.body());
                }
            } else if (this.debugMode.get()) {
                info("%s webhook sent successfully - Status: %d", messageType, response.statusCode());
            }
        } catch (Exception e) {
            if (this.debugMode.get() || this.notifications.get()) {
                ChatUtils.error("%s webhook error: %s", messageType, e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private String createSaleEmbed(String itemName, double salePrice) {
        String playerName = this.mc.player != null ? this.mc.player.getName().getString() : "Unknown";
        long timestamp = System.currentTimeMillis() / 1000L;

        String pingContent = "";
        if (this.selfPing.get() && !this.discordId.get().trim().isEmpty()) {
            pingContent = String.format("<@%s> ", this.discordId.get().trim());
        }

        String webhookUsernameHardcoded = "Glazed AH Seller";
        String webhookAvatarUrlHardcoded = "https://i.imgur.com/OL2y1cr.png";
        String webhookThumbnailUrlHardcoded = "https://i.imgur.com/OL2y1cr.png";

        String messageContent = String.format("%s💰 **%s** sold **%s** for **%s**!", pingContent, playerName, itemName, formatPrice(salePrice));
        String salePriceStr = formatPrice(salePrice);

        return String.format("{\"content\":\"%s\",\"username\":\"%s\",\"avatar_url\":\"%s\",\"embeds\":[{\"title\":\"Glazed AH Seller Alert\",\"description\":\"Item successfully listed and awaiting sale.\",\"color\":65280,\"thumbnail\":{\"url\":\"%s\"},\"fields\":[{\"name\":\"📦 Item\",\"value\":\"%s\",\"inline\":true},{\"name\":\"💵 Sale Price\",\"value\":\"%s\",\"inline\":true},{\"name\":\"⏰ Timestamp\",\"value\":\"<t:%d:R>\",\"inline\":true}],\"footer\":{\"text\":\"Glazed AH Seller V1\"},\"timestamp\":\"%s\"}]}",
            this.escapeJson(messageContent), this.escapeJson(webhookUsernameHardcoded), this.escapeJson(webhookAvatarUrlHardcoded),
            this.escapeJson(webhookThumbnailUrlHardcoded), this.escapeJson(itemName), this.escapeJson(salePriceStr), timestamp, Instant.now().toString());
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
