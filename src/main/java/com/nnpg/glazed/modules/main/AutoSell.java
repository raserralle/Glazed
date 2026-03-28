package com.nnpg.glazed.modules.main;

import com.nnpg.glazed.GlazedAddon;
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

import java.util.List;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWebhook = settings.createGroup("Discord Webhook");

    private final Setting<SellMode> mode = sgGeneral.add(new EnumSetting.Builder<SellMode>()
        .name("mode")
        .description("Whether to whitelist or blacklist the selected items.")
        .defaultValue(SellMode.Whitelist)
        .build()
    );

    private final Setting<List<Item>> itemList = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to sell.")
        .defaultValue(List.of(Items.SEA_PICKLE))
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between actions.")
        .defaultValue(1)
        .min(0)
        .max(20)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show chat feedback.")
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

    private int delayCounter;
    private boolean shouldReopen = false;
    private String lastSoldItemName = "";
    private double lastSoldPrice = 0;
    private final HttpClient httpClient;

    public AutoSell() {
        super(GlazedAddon.CATEGORY, "auto-sell", "Automatically sells items.");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
    }

    @Override
    public void onActivate() {
        delayCounter = 20;
        shouldReopen = false;
    }

    @Override
    public void onDeactivate() {
        shouldReopen = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (delayCounter > 0) {
            delayCounter--;
            return;
        }

        if (shouldReopen) {
            mc.getNetworkHandler().sendChatCommand("sell");
            shouldReopen = false;
            delayCounter = delay.get();
            return;
        }

        handleSellMode();
    }

    @EventHandler
    private void onChatMessage(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();

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

    private void handleSellMode() {
        ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;

        if (!(currentScreenHandler instanceof GenericContainerScreenHandler)) {
            mc.getNetworkHandler().sendChatCommand("sell");
            delayCounter = 20;
            return;
        }

        if (areAllSellSlotsOccupied(currentScreenHandler)) {
            if (notifications.get()) info("All sell menu slots (0-35) are occupied. Closing and reopening menu.");
            mc.player.closeHandledScreen();
            shouldReopen = true;
            delayCounter = delay.get();
            return;
        }

        int totalSlots = currentScreenHandler.slots.size();
        boolean foundItemToSell = false;

        for (int slot = 36; slot < totalSlots; slot++) {
            ItemStack stack = currentScreenHandler.getSlot(slot).getStack();

            if (stack.isEmpty()) continue;

            Item itemInSlot = stack.getItem();
            if (!shouldSellItem(itemInSlot)) continue;

            foundItemToSell = true;
            this.lastSoldItemName = stack.getName().getString();
            this.lastSoldPrice = 0; // Price will be extracted from chat message when item sells
            mc.interactionManager.clickSlot(currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
            delayCounter = delay.get();
            return;
        }

        if (!foundItemToSell) {
            if (notifications.get()) info("All items sold. Closing GUI.");
            mc.player.closeHandledScreen();
            toggle();
            delayCounter = 40;
        }
    }

    private boolean areAllSellSlotsOccupied(ScreenHandler screenHandler) {
        for (int slot = 0; slot <= 35; slot++) {
            if (slot >= screenHandler.slots.size()) {
                return false; // If we don't have enough slots, they can't all be occupied
            }

            ItemStack stack = screenHandler.getSlot(slot).getStack();
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldSellItem(Item item) {
        List<Item> selectedItems = itemList.get();

        if (mode.get() == SellMode.Whitelist) {
            return selectedItems.contains(item);
        } else {
            return !selectedItems.contains(item);
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
                .header("User-Agent", "AutoSell/1.0")
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

        String webhookUsernameHardcoded = "Glazed AutoSeller";
        String webhookAvatarUrlHardcoded = "https://imgur.com/PHRZBjd";
        String webhookThumbnailUrlHardcoded = "https://imgur.com/PHRZBjd";

        String messageContent = String.format("%s💰 **%s** sold **%s** for **%s**!", pingContent, playerName, itemName, formatPrice(salePrice));
        String salePriceStr = formatPrice(salePrice);

        return String.format("{\"content\":\"%s\",\"username\":\"%s\",\"avatar_url\":\"%s\",\"embeds\":[{\"title\":\"Glazed AutoSeller Alert\",\"description\":\"Item sold through AutoSell.\",\"color\":65280,\"thumbnail\":{\"url\":\"%s\"},\"fields\":[{\"name\":\"📦 Item\",\"value\":\"%s\",\"inline\":true},{\"name\":\"💵 Sale Price\",\"value\":\"%s\",\"inline\":true},{\"name\":\"⏰ Timestamp\",\"value\":\"<t:%d:R>\",\"inline\":true}],\"footer\":{\"text\":\"Glazed AutoSeller V1\"},\"timestamp\":\"%s\"}]}",
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

    public void info(String message, Object... args) {
        ChatUtils.info(String.format(message, args));
    }

    public void warning(String message, Object... args) {
        ChatUtils.warning(String.format(message, args));
    }

    public void error(String message, Object... args) {
        ChatUtils.error(String.format(message, args));
    }

    public enum SellMode {
        Whitelist,
        Blacklist
    }
}
