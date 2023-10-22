package com.leon.bugreport;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
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
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.leon.bugreport.BugReportManager.pluginColor;
import static com.leon.bugreport.BugReportManager.pluginTitle;

public class BugReportSettings {
    public BugReportSettings(BugReportManager reportManager) { }

    public static Inventory getSettingsGUI() {
        int secondRow = 9;
        int thirdRow = 18;
        int fifthRow = 36;

        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("settings"));

        ItemStack setDiscordWebhook = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGQ0MjMzN2JlMGJkY2EyMTI4MDk3ZjFjNWJiMTEwOWU1YzYzM2MxNzkyNmFmNWZiNmZjMjAwMDAwMTFhZWI1MyJ9fX0=", BugReportLanguage.getTitleFromLanguage("enableDiscordWebhook"), 1);
        ItemStack setLanguage = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZTU0Y2JlODc4NjdkMTRiMmZiZGYzZjE4NzA4OTQzNTIwNDhkZmVjZDk2Mjg0NmRlYTg5M2IyMTU0Yzg1In19fQ==", BugReportLanguage.getTitleFromLanguage("setLanguage"), 2);

        ItemStack maxReportsPerPlayer = createButton(Material.PAPER, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("setMaxReportsPerPlayer"));
        ItemStack toggleCategorySelection = createButton(Material.CHEST, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enableCategorySelection"));
        ItemStack setBugReportNotifications = createButton(Material.BELL, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enableBugReportNotifications"));
        ItemStack onIcon = createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on"));
        ItemStack offIcon = createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off"));

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

        gui.setItem(secondRow + 1, setDiscordWebhook);
        gui.setItem(secondRow + 2, setBugReportNotifications);
        gui.setItem(secondRow + 3, toggleCategorySelection);
        gui.setItem(secondRow + 4, maxReportsPerPlayer);
        gui.setItem(secondRow + 5, setLanguage);
        gui.setItem(thirdRow + 1, getDiscordWebhookToggle() ? onIcon : offIcon);
        gui.setItem(thirdRow + 2, getBugReportNotificationsToggle() ? onIcon : offIcon);
        gui.setItem(thirdRow + 3, getCategorySelectionToggle() ? onIcon : offIcon);

        gui.setItem(fifthRow + 4, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("close")));

        return gui;
    }

    static ItemStack createButton(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean getDiscordWebhookToggle() {
        return BugReportManager.config.getBoolean("enableDiscordWebhook");
    }

    private static void setDiscordWebhookToggle(Player player) {
        boolean toggle = getDiscordWebhookToggle();
        BugReportManager.config.set("enableDiscordWebhook", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(19, getDiscordWebhookToggle() ? createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")) : createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")));
    }

    private static boolean getBugReportNotificationsToggle() {
        return BugReportManager.config.getBoolean("enableBugReportNotifications");
    }

    private static void setBugReportNotificationsToggle(Player player) {
        boolean toggle = getBugReportNotificationsToggle();
        BugReportManager.config.set("enableBugReportNotifications", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(20, toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")) : createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")));
    }

    private static boolean getCategorySelectionToggle() {
        return BugReportManager.config.getBoolean("enablePluginReportCategories");
    }

    private static void setCategorySelectionToggle(Player player) {
        boolean toggle = getCategorySelectionToggle();
        BugReportManager.config.set("enablePluginReportCategories", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(21, toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("off")) : createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("on")));
    }

    private static void setLanguageToggle(Player player) {
        player.openInventory(openLanguageGUI());
    }

    private static Inventory openLanguageGUI() {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("language"));

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

        gui.setItem(20, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODgzMWM3M2Y1NDY4ZTg4OGMzMDE5ZTI4NDdlNDQyZGZhYTg4ODk4ZDUwY2NmMDFmZDJmOTE0YWY1NDRkNTM2OCJ9fX0=", "English", 11));
        gui.setItem(21, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTEyNjlhMDY3ZWUzN2U2MzYzNWNhMWU3MjNiNjc2ZjEzOWRjMmRiZGRmZjk2YmJmZWY5OWQ4YjM1Yzk5NmJjIn19fQ==", "French", 12));
        gui.setItem(22, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ==", "German", 13));
        gui.setItem(23, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzJkNzMwYjZkZGExNmI1ODQ3ODNiNjNkMDgyYTgwMDQ5YjVmYTcwMjI4YWJhNGFlODg0YzJjMWZjMGMzYThiYyJ9fX0=", "Spanish", 14));
        gui.setItem(24, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODVjZTg5MjIzZmE0MmZlMDZhZDY1ZDhkNDRjYTQxMmFlODk5YzgzMTMwOWQ2ODkyNGRmZTBkMTQyZmRiZWVhNCJ9fX0=", "Italian", 15));
        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));

        return gui;
    }

    public static ItemStack createCustomPlayerHead(String textureValue, String displayName, int ID) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        GameProfile profile = new GameProfile(java.util.UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", textureValue));

        try {
            java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        skullMeta.setDisplayName(displayName);
        skullMeta.setCustomModelData(ID);
        playerHead.setItemMeta(skullMeta);
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

                if (clickedItem.getItemMeta().hasCustomModelData()) {
                    if (clickedItem.getItemMeta().getCustomModelData() == 1) {
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
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + DefaultLanguageSelector.getTextElseDefault(BugReportManager.language, "enterMaxReportsPerPlayer"));

                        setMaxReportsClickMap.put(player.getUniqueId(), String.valueOf(true));
                        setMaxReportsClickMap.put(player.getUniqueId(), customItemDisplayName);
                    }
                    case "Set Report Cooldown" -> {
                        player.closeInventory();
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + "Enter the cooldown between reports in seconds. Or type 'cancel' to cancel."); // TODO: Language support

                        setReportCooldownClickMap.put(player.getUniqueId(), String.valueOf(true));
                        setReportCooldownClickMap.put(player.getUniqueId(), customItemDisplayName);
                    }
                }
            }

            if (customDisplayName.contains("Language")) {
                event.setCancelled (true);

                Player player = (Player) event.getWhoClicked ();
                Inventory clickedInventory = event.getClickedInventory ();
                ItemStack clickedItem = event.getCurrentItem ();

                if (clickedInventory == null || clickedItem == null || clickedItem.getType () == Material.AIR) {
                    return;
                }

                ItemMeta itemMeta = clickedItem.getItemMeta ();

                if (itemMeta == null || !itemMeta.hasDisplayName ()) {
                    return;
                }

                String itemDisplayName = itemMeta.getDisplayName ();
                String customItemDisplayName = BugReportLanguage.getEnglishVersionFromLanguage (itemDisplayName);

                if (customItemDisplayName.equals ("Back")) {
                    player.openInventory (getSettingsGUI ());
                    return;
                }

                if (clickedItem.getItemMeta ().hasCustomModelData ()) {
                    int customModelData = clickedItem.getItemMeta ().getCustomModelData ();
                    switch (customModelData) {
                        case 11 -> setLanguage ("en", "English", player);
                        case 12 -> setLanguage ("fr", "French", player);
                        case 13 -> setLanguage ("de", "German", player);
                        case 14 -> setLanguage ("es", "Spanish", player);
                        case 15 -> setLanguage ("it", "Italian", player);
                    }
                }
            }
		}

        private static void setLanguage(String languageCode, String languageName, Player player) {
            player.closeInventory();
            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + DefaultLanguageSelector.getTextElseDefault(languageCode, "languageSetTo").replace("%language%", languageName));
            BugReportManager.config.set("language", languageCode);
            BugReportManager.saveConfig();
            BugReportManager.loadConfig();
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();

            if (setMaxReportsClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setMaxReportsClickMap, BugReportLanguage.getTitleFromLanguage("setMaxReportsPerPlayer"),
                        "max-reports-per-player", (value) -> {
                            int maxReports;

                            if (value.matches("[0-9]+")) {
                                try {
                                    maxReports = Integer.parseInt(value);
                                } catch (NumberFormatException e) {
                                    player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + DefaultLanguageSelector.getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"));
                                    return;
                                }

                                BugReportManager.config.set("max-reports-per-player", maxReports);
                                BugReportManager.saveConfig();
                                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + DefaultLanguageSelector.getTextElseDefault(BugReportManager.config.getString("language"), "maxReportsPerPlayerSuccessMessage").replace("%amount%", String.valueOf(maxReports)));
                            } else {
                                value = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
                                String customDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(value);

                                if (customDisplayName.contains("Cancel")) {
                                    player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("cancelled"));
                                } else {
                                    player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + DefaultLanguageSelector.getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"));
                                }
                            }
                        });
            } else if (setReportCooldownClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setReportCooldownClickMap, "Set Report Cooldown",
                        "report-cooldown", (value) -> {
                            int reportCooldown;
                            try {
                                reportCooldown = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + DefaultLanguageSelector.getTextElseDefault(BugReportManager.config.getString("language"), "enterValidNumber"));
                                return;
                            }

                            BugReportManager.config.set("report-cooldown", reportCooldown);
                            BugReportManager.saveConfig();
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + DefaultLanguageSelector.getTextElseDefault(BugReportManager.config.getString("language"), "reportCooldownSuccessMessage")
                                    .replace("%time%", String.valueOf(reportCooldown)));
                        });
            }
        }

        private void handleSettingUpdate(AsyncPlayerChatEvent event, Player player, Map<UUID, String> settingClickMap, String displayName, String configKey, Consumer<String> updateLogic) {
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