package com.leon.bugreport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.DefaultLanguageSelector.getTextElseDefault;

public class BugReportSettings {
    public BugReportSettings(BugReportManager reportManager) { }

    public static @NotNull Inventory getSettingsGUI() {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("settings"));

        ItemStack setDiscordWebhook = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGQ0MjMzN2JlMGJkY2EyMTI4MDk3ZjFjNWJiMTEwOWU1YzYzM2MxNzkyNmFmNWZiNmZjMjAwMDAwMTFhZWI1MyJ9fX0=", BugReportLanguage.getTitleFromLanguage("enableDiscordWebhook"), 1);
        ItemStack setLanguage = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZTU0Y2JlODc4NjdkMTRiMmZiZGYzZjE4NzA4OTQzNTIwNDhkZmVjZDk2Mjg0NmRlYTg5M2IyMTU0Yzg1In19fQ==", BugReportLanguage.getTitleFromLanguage("setLanguage"), 2);

        ItemStack maxReportsPerPlayer = createButton(Material.PAPER, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("setMaxReportsPerPlayer"));
        ItemStack toggleCategorySelection = createButton(Material.CHEST, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enableCategorySelection"));
        ItemStack setBugReportNotifications = createButton(Material.BELL, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enableBugReportNotifications"));
        ItemStack onIcon = createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true"));
        ItemStack offIcon = createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false"));
        ItemStack otherSettings = createButton(Material.BOOK, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("otherSettings"));

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        for (int i = 36; i < 45; i++) {
            gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        for (int i = 9; i < 36; i++) {
            if (i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        gui.setItem(10, setDiscordWebhook);
        gui.setItem(11, setBugReportNotifications);
        gui.setItem(12, toggleCategorySelection);
        gui.setItem(13, maxReportsPerPlayer);
        gui.setItem(14, setLanguage);
        gui.setItem(15, otherSettings);

        gui.setItem(19, getDiscordWebhookToggle() ? onIcon : offIcon);
        gui.setItem(20, getBugReportNotificationsToggle() ? onIcon : offIcon);
        gui.setItem(21, getCategorySelectionToggle() ? onIcon : offIcon);

        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("close")));

        return gui;
    }

    static @NotNull ItemStack createButton(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean getDiscordWebhookToggle() {
        return BugReportManager.config.getBoolean("enableDiscordWebhook");
    }

    private static void setDiscordWebhookToggle(@NotNull Player player) {
        boolean toggle = getDiscordWebhookToggle();
        BugReportManager.config.set("enableDiscordWebhook", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(19, getDiscordWebhookToggle() ? createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")) : createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")));
    }

    private static boolean getBugReportNotificationsToggle() {
        return BugReportManager.config.getBoolean("enableBugReportNotifications");
    }

    private static void setBugReportNotificationsToggle(@NotNull Player player) {
        boolean toggle = getBugReportNotificationsToggle();
        BugReportManager.config.set("enableBugReportNotifications", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(20, toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")) : createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")));
    }

    private static boolean getCategorySelectionToggle() {
        return BugReportManager.config.getBoolean("enablePluginReportCategories");
    }

    private static void setCategorySelectionToggle(@NotNull Player player) {
        boolean toggle = getCategorySelectionToggle();
        BugReportManager.config.set("enablePluginReportCategories", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(21, toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")) : createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")));
    }

    private static void setLanguageToggle(@NotNull Player player) {
        player.openInventory(openLanguageGUI());
    }

    private static @NotNull Inventory openLanguageGUI() {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("language"));

        for (int i = 36; i < 45; i++) {
            gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

		gui.setItem(19-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODgzMWM3M2Y1NDY4ZTg4OGMzMDE5ZTI4NDdlNDQyZGZhYTg4ODk4ZDUwY2NmMDFmZDJmOTE0YWY1NDRkNTM2OCJ9fX0=", "English",              11));
        gui.setItem(20-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTEyNjlhMDY3ZWUzN2U2MzYzNWNhMWU3MjNiNjc2ZjEzOWRjMmRiZGRmZjk2YmJmZWY5OWQ4YjM1Yzk5NmJjIn19fQ==", "French",               12));
        gui.setItem(21-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ==", "German",               13));
        gui.setItem(22-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzJkNzMwYjZkZGExNmI1ODQ3ODNiNjNkMDgyYTgwMDQ5YjVmYTcwMjI4YWJhNGFlODg0YzJjMWZjMGMzYThiYyJ9fX0=", "Spanish",              14));
        gui.setItem(23-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODVjZTg5MjIzZmE0MmZlMDZhZDY1ZDhkNDRjYTQxMmFlODk5YzgzMTMwOWQ2ODkyNGRmZTBkMTQyZmRiZWVhNCJ9fX0=", "Italian",              15));
        gui.setItem(24-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2Y5YmMwMzVjZGM4MGYxYWI1ZTExOThmMjlmM2FkM2ZkZDJiNDJkOWE2OWFlYjY0ZGU5OTA2ODE4MDBiOThkYyJ9fX0=", "Simplified Chinese",   16));
        gui.setItem(25-9, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTZlYWZlZjk4MGQ2MTE3ZGFiZTg5ODJhYzRiNDUwOTg4N2UyYzQ2MjFmNmE4ZmU1YzliNzM1YTgzZDc3NWFkIn19fQ==", "Russian",              17));

        String language = BugReportManager.config.getString("language");

        for (int i = 28-9; i < 35-9; i++) {
            gui.setItem(i, createButton(Material.GRAY_DYE, BugReportLanguage.getTitleFromLanguage("off")));
        }

        switch (Objects.requireNonNull(language)) {
            case "en" -> gui.setItem(28-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
            case "fr" -> gui.setItem(29-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
            case "de" -> gui.setItem(30-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
            case "es" -> gui.setItem(31-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
            case "it" -> gui.setItem(32-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
            case "zh" -> gui.setItem(33-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
            case "ru" -> gui.setItem(34-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("on")));
        }

        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));

        return gui;
    }

    public static @NotNull ItemStack createCustomPlayerHead(String texture, String name, int modelData) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        if (skullMeta != null) {
            try {
                String decodedValue = new String(Base64.getDecoder().decode(texture));
                JsonObject textureJson = JsonParser.parseString(decodedValue).getAsJsonObject();
                String textureUrl = textureJson.getAsJsonObject("textures")
                   .getAsJsonObject("SKIN")
                   .get("url").getAsString();

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();

				textures.setSkin(new URL(textureUrl));
				profile.setTextures(textures);
				skullMeta.setOwnerProfile(profile);

				skullMeta.setDisplayName(name);
                skullMeta.setCustomModelData(modelData);
                playerHead.setItemMeta(skullMeta);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create custom player head: " + e.getMessage());
                return new ItemStack(Material.PLAYER_HEAD);
            }
        }

        return playerHead;
    }

    public static class BugReportSettingsListener implements Listener {
        private final Map<UUID, String> setMaxReportsClickMap = new HashMap<>();
        private final Map<UUID, String> setReportCooldownClickMap = new HashMap<>();

        @EventHandler(priority = EventPriority.NORMAL)
        public void onInventoryClick(@NotNull InventoryClickEvent event) {
            String displayName = ChatColor.stripColor(event.getView().getTitle());
            if (displayName.contains("Bug Report - ")) {
                displayName = displayName.substring(13);
            }

            String customDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(displayName);

            if (customDisplayName.contains("Settings")) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                Inventory clickedInventory = event.getClickedInventory();
                if (clickedInventory == null) {
                    return;
                }

                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                ItemMeta itemMeta = clickedItem.getItemMeta();
                if (itemMeta == null || !itemMeta.hasDisplayName()) {
                    return;
                }

                String itemDisplayName = itemMeta.getDisplayName();
                String customItemDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(itemDisplayName);

                if (customItemDisplayName.equals("Close")) {
                    player.closeInventory();
                    return;
                }

                if (clickedItem.getItemMeta ().hasCustomModelData()) {
                    if (clickedItem.getItemMeta ().getCustomModelData() == 1) {
                        setDiscordWebhookToggle(player);
                    } else if (clickedItem.getItemMeta().getCustomModelData() == 2) {
                        setLanguageToggle(player);
                    }
                }

                switch (customItemDisplayName) {
                    case "Enable Bug Report Notifications" -> setBugReportNotificationsToggle(player);
                    case "Enable Category Selection" -> setCategorySelectionToggle(player);
                    case "Set Max Reports Per Player" -> {
                        player.closeInventory();
                        if (BugReportManager.config.getBoolean("useTitleInsteadOfMessage")) {
                            player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(BugReportManager.language, "enterMaxReportsPerPlayer"), 10, 70, 20);
                        } else {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + getTextElseDefault(BugReportManager.language, "enterMaxReportsPerPlayer"));
                        }
                        setMaxReportsClickMap.put(player.getUniqueId(), String.valueOf(true));
                        setMaxReportsClickMap.put(player.getUniqueId(), customItemDisplayName);
                    }
                    case "Set Report Cooldown" -> {
                        player.closeInventory();
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + "Enter the cooldown between reports in seconds. Or type 'cancel' to cancel."); // TODO: Language support

                        setReportCooldownClickMap.put(player.getUniqueId(), String.valueOf(true));
                        setReportCooldownClickMap.put(player.getUniqueId(), customItemDisplayName);
                    }
                    case "Other Settings" -> player.openInventory(getOtherSettingsGUI());
                }
            }

            if (customDisplayName.contains("Language")) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                Inventory clickedInventory = event.getClickedInventory();
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                ItemMeta itemMeta = clickedItem.getItemMeta();

                if (itemMeta == null || !itemMeta.hasDisplayName()) {
                    return;
                }

                String itemDisplayName = itemMeta.getDisplayName();
                String customItemDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(itemDisplayName);

                if (customItemDisplayName.equals("Back")) {
                    player.openInventory(getSettingsGUI());
                    return;
                }

                if (clickedItem.getItemMeta().hasCustomModelData()) {
                    int customModelData = clickedItem.getItemMeta().getCustomModelData();
                    switch (customModelData) {
                        case 11 -> setLanguage("en", "English", player);
                        case 12 -> setLanguage("fr", "French", player);
                        case 13 -> setLanguage("de", "German", player);
                        case 14 -> setLanguage("es", "Spanish", player);
                        case 15 -> setLanguage("it", "Italian", player);
                        case 16 -> setLanguage("zh", "Simplified Chinese", player);
                        case 17 -> setLanguage("ru", "Russian", player);
                    }
                }
            }

            if (customDisplayName.contains("Other Settings")) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                Inventory clickedInventory = event.getClickedInventory();
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                ItemMeta itemMeta = clickedItem.getItemMeta();

                if (itemMeta == null || !itemMeta.hasDisplayName()) {
                    return;
                }

                String itemDisplayName = itemMeta.getDisplayName();
                String customItemDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(itemDisplayName);

                if (customItemDisplayName.equals("Back")) {
                    player.openInventory(getSettingsGUI());
                    return;
                }

                switch (customItemDisplayName) {
                    case "Enable Title Message" -> setTitleMessage(player);
                    case "Enable Player Heads" -> setPlayerHead(player);
                }
            }
        }

        private Inventory getOtherSettingsGUI() {
            int secondRow = 9;
            int thirdRow = 18;
            Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("otherSettings"));

            for (int i = 0; i < 9; i++) {
                gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
            }

            for (int i = 36; i < 45; i++) {
                gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
            }

            for (int i = 9; i < 36; i++) {
                if (i % 9 == 0 || i % 9 == 8) {
                    gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
                }
            }

            ItemStack enableTitleMessage = createButton(Material.PAPER, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enableTitleMessage"));
            ItemStack enablePlayerHeads = createButton(Material.PLAYER_HEAD, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enablePlayerHeads"));
            ItemStack onIcon = createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on"));
            ItemStack offIcon = createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off"));

            gui.setItem(secondRow + 1, enableTitleMessage);
            gui.setItem(secondRow + 2, enablePlayerHeads);

            gui.setItem(thirdRow + 1, getTitleMessage() ? onIcon : offIcon);
            gui.setItem(thirdRow + 2, getPlayerHead() ? onIcon : offIcon);

            gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));

            return gui;
        }

        private boolean getTitleMessage() {
            return BugReportManager.config.getBoolean("useTitleInsteadOfMessage");
        }

        private void setTitleMessage(@NotNull Player player) {
            boolean toggle = getTitleMessage();
            BugReportManager.config.set("useTitleInsteadOfMessage", !toggle);
            BugReportManager.saveConfig();
            player.getOpenInventory().setItem(19, getTitleMessage() ? createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")) : createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")));
        }

        private boolean getPlayerHead() {
            return BugReportManager.config.getBoolean("enablePlayerHeads");
        }

        private void setPlayerHead(@NotNull Player player) {
            boolean toggle = getPlayerHead();
            BugReportManager.config.set("enablePlayerHeads", !toggle);
            BugReportManager.saveConfig();
            player.getOpenInventory().setItem(20, getPlayerHead() ? createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")) : createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")));
        }

        private static void setLanguage(String languageCode, String languageName, @NotNull Player player) {
            player.closeInventory();
            if (checkForKey("useTitleInsteadOfMessage", true)) {
                player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(languageCode, "languageSetTo").replace("%language%", languageName), 10, 70, 20);
            } else {
                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + getTextElseDefault(languageCode, "languageSetTo").replace("%language%", languageName));
            }
            BugReportManager.config.set("language", languageCode);
            BugReportManager.saveConfig();
            BugReportManager.loadConfig();
            player.openInventory(openLanguageGUI());
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onPlayerChat(@NotNull AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();

            if (setMaxReportsClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setMaxReportsClickMap, BugReportLanguage.getTitleFromLanguage("setMaxReportsPerPlayer"), "max-reports-per-player", (value) -> {
                    int maxReports;
                    if (value.matches("[0-9]+")) {
                        try {
                            maxReports = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            if (checkForKey("useTitleInsteadOfMessage", true)) {
                                player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"), 10, 70, 20);
                            } else {
                                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"));
                            }
                            return;
                        }
                        BugReportManager.config.set("max-reports-per-player", maxReports);
                        BugReportManager.saveConfig();
                        if (checkForKey("useTitleInsteadOfMessage", true)) {
                            player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(BugReportManager.config.getString("language"), "maxReportsPerPlayerSuccessMessage")
                                    .replace("%amount%", String.valueOf(maxReports)), 10, 70, 20);
                        } else {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + getTextElseDefault(BugReportManager.config.getString("language"), "maxReportsPerPlayerSuccessMessage")
                                    .replace("%amount%", String.valueOf(maxReports)));
                        }
                    } else {
                        value = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
                        String customDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(value);
                        if (customDisplayName.contains("Cancel")) {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("cancelled"));
                        } else {
                            if (checkForKey("useTitleInsteadOfMessage", true)) {
                                player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"), 10, 70, 20);
                            } else {
                                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"));
                            }
                        }
                    }
                });
            } else if (setReportCooldownClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setReportCooldownClickMap, "Set Report Cooldown", "report-cooldown", (value) -> {
                    int reportCooldown;
                    try {
                        reportCooldown = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        if (checkForKey("useTitleInsteadOfMessage", true)) {
                            player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"), 10, 70, 20);
                        } else {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"));
                        }
                        return;
                    }

                    BugReportManager.config.set("report-cooldown", reportCooldown);
                    BugReportManager.saveConfig();
                    if (checkForKey("useTitleInsteadOfMessage", true)) {
                        player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(BugReportManager.config.getString("language"), "reportCooldownSuccessMessage")
                            .replace("%time%", String.valueOf(reportCooldown)), 10, 70, 20);
                    } else {
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + getTextElseDefault(BugReportManager.config.getString("language"), "reportCooldownSuccessMessage")
                                .replace("%time%", String.valueOf(reportCooldown)));
                    }
                });
            }
        }

        private void handleSettingUpdate(AsyncPlayerChatEvent event, @NotNull Player player, @NotNull Map<UUID, String> settingClickMap, String displayName, String configKey, Consumer<String> updateLogic) {
            String clickDisplayName = settingClickMap.get(player.getUniqueId());
            String displayNameDefault = BugReportLanguage.getEnglishVersionFromLanguage(displayName);

            if (clickDisplayName != null && clickDisplayName.equals(displayNameDefault)) {
                event.setCancelled(true);

                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.runTask(BugReportManager.plugin, () -> {
                    updateLogic.accept(event.getMessage());
                    player.openInventory(getSettingsGUI());
                    settingClickMap.remove(player.getUniqueId());
                });
            }
        }
    }
}