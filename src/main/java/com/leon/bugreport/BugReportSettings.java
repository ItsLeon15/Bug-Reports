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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class BugReportSettings {

    private final BugReportManager reportManager;

    public BugReportSettings(BugReportManager reportManager) {
        this.reportManager = reportManager;
    }

    public static Inventory getSettingsGUI() {
        int secondRow = 9;
        int thirdRow = 18;
        int fifthRow = 36;

        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report Settings");

        ItemStack setDiscordWebhook = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGQ0MjMzN2JlMGJkY2EyMTI4MDk3ZjFjNWJiMTEwOWU1YzYzM2MxNzkyNmFmNWZiNmZjMjAwMDAwMTFhZWI1MyJ9fX0=", "Enable Discord Webhook", 1);
//        ItemStack setLanguage = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZTU0Y2JlODc4NjdkMTRiMmZiZGYzZjE4NzA4OTQzNTIwNDhkZmVjZDk2Mjg0NmRlYTg5M2IyMTU0Yzg1In19fQ==", "Set Language", 2);

        ItemStack maxReportsPerPlayer = createButton(Material.PAPER, ChatColor.YELLOW + "Set Max Reports Per Player");
        ItemStack toggleCategorySelection = createButton(Material.CHEST, ChatColor.YELLOW + "Enable Category Selection");
//        ItemStack setReportCooldown = createButton(Material.CLOCK, ChatColor.YELLOW + "Set Report Cooldown");
        ItemStack setReportConfirmationMessage = createButton(Material.BOOK, ChatColor.YELLOW + "Set Report Confirmation Message");
        ItemStack setBugReportNotifications = createButton(Material.BELL, ChatColor.YELLOW + "Enable Bug Report Notifications");

        ItemStack onIcon = createButton(Material.LIME_DYE, ChatColor.GREEN + "On");
        ItemStack offIcon = createButton(Material.GRAY_DYE, ChatColor.RED + "Off");

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
        gui.setItem(secondRow + 5, setReportConfirmationMessage);
//        gui.setItem(secondRow + 5, setReportCooldown);
//        gui.setItem(secondRow + 7, setLanguage);

        gui.setItem(thirdRow + 1, getDiscordWebhookToggle() ? onIcon : offIcon);
        gui.setItem(thirdRow + 2, getBugReportNotificationsToggle() ? onIcon : offIcon);
        gui.setItem(thirdRow + 3, getCategorySelectionToggle() ? onIcon : offIcon);

        gui.setItem(fifthRow + 4, createButton(Material.BARRIER, ChatColor.RED + "Back"));

        return gui;
    }

    private static ItemStack createButton(Material material, String displayName) {
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
        player.getOpenInventory().setItem(19, getDiscordWebhookToggle() ? createButton(Material.LIME_DYE, ChatColor.GREEN + "On") : createButton(Material.GRAY_DYE, ChatColor.RED + "Off"));
    }

    private static boolean getBugReportNotificationsToggle() {
        return BugReportManager.config.getBoolean("enableBugReportNotifications");
    }

    private static void setBugReportNotificationsToggle(Player player) {
        boolean toggle = getBugReportNotificationsToggle();
        BugReportManager.config.set("enableBugReportNotifications", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(20, toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + "Off") : createButton(Material.LIME_DYE, ChatColor.GREEN + "On"));
    }

    private static boolean getCategorySelectionToggle() {
        return BugReportManager.config.getBoolean("enablePluginReportCategories");
    }

    private static void setCategorySelectionToggle(Player player) {
        boolean toggle = getCategorySelectionToggle();
        BugReportManager.config.set("enablePluginReportCategories", !toggle);
        BugReportManager.saveConfig();
        player.getOpenInventory().setItem(21, toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + "Off") : createButton(Material.LIME_DYE, ChatColor.GREEN + "On"));
    }

    private static void setLanguageToggle(Player player) {
        player.openInventory(openLanguageGUI());
    }

    private static Inventory openLanguageGUI() {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report Language");

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
        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + "Back"));

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
        private final Map<UUID, String> setReportConfirmationMessageClickMap = new HashMap<>();

        @EventHandler(priority = EventPriority.NORMAL)
        public Object onInventoryClick(InventoryClickEvent event) {
            if (event.getView().getTitle().startsWith(ChatColor.YELLOW + "Bug Report Settings")) {

                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                Inventory clickedInventory = event.getClickedInventory();
                if (clickedInventory == null) {
                    return null;
                }

                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return null;
                }

                ItemMeta itemMeta = clickedItem.getItemMeta();
                if (itemMeta == null || !itemMeta.hasDisplayName()) {
                    return null;
                }

                String displayName = itemMeta.getDisplayName();

                if (displayName.equals(ChatColor.RED + "Back")) {
                    player.closeInventory();
                    player.openInventory(BugReportManager.getBugReportGUI(player));
                    return null;
                }

                if (clickedItem.getItemMeta().hasCustomModelData()) {
                    if (clickedItem.getItemMeta().getCustomModelData() == 1) {
                        setDiscordWebhookToggle(player);
                    } else if (clickedItem.getItemMeta().getCustomModelData() == 2) {
                        setLanguageToggle(player);
                    }
                }

                if (displayName.equals(ChatColor.YELLOW + "Enable Bug Report Notifications")) {
                    setBugReportNotificationsToggle(player);
                } else if (displayName.equals(ChatColor.YELLOW + "Enable Category Selection")) {
                    setCategorySelectionToggle(player);
                } else if (displayName.equals(ChatColor.YELLOW + "Set Max Reports Per Player")) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Enter the max reports a player can submit. Or type 'cancel' to cancel.");

                    setMaxReportsClickMap.put(player.getUniqueId(), String.valueOf(true));
                    setMaxReportsClickMap.put(player.getUniqueId(), displayName);
                } else if (displayName.equals(ChatColor.YELLOW + "Set Report Cooldown")) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Enter the cooldown between reports in seconds. Or type 'cancel' to cancel.");

                    setReportCooldownClickMap.put(player.getUniqueId(), String.valueOf(true));
                    setReportCooldownClickMap.put(player.getUniqueId(), displayName);
                } else if (displayName.equals(ChatColor.YELLOW + "Set Report Confirmation Message")) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Enter the message to send to the player when they submit a report. Or type 'cancel' to cancel.");

                    setReportConfirmationMessageClickMap.put(player.getUniqueId(), String.valueOf(true));
                    setReportConfirmationMessageClickMap.put(player.getUniqueId(), displayName);
                }
            }

            if (event.getView().getTitle().startsWith(ChatColor.YELLOW + "Bug Report Language")) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                Inventory clickedInventory = event.getClickedInventory();
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return null;
                }

                ItemMeta itemMeta = clickedItem.getItemMeta();

                if (itemMeta == null || !itemMeta.hasDisplayName()) {
                    return null;
                }

                String displayName = itemMeta.getDisplayName();

                if (displayName.equals(ChatColor.RED + "Back")) {
                    player.openInventory(getSettingsGUI());
                    return null;
                }

                if (clickedItem.getItemMeta().hasCustomModelData()) {
                    int customModelData = clickedItem.getItemMeta().getCustomModelData();
                    switch (customModelData) {
                        case 11 -> setLanguage("en", "English", player);
                        case 12 -> setLanguage("fr", "French", player);
                        case 13 -> setLanguage("de", "German", player);
                        case 14 -> setLanguage("es", "Spanish", player);
                        case 15 -> setLanguage("it", "Italian", player);
                    }
                }
            }
            return null;
        }

        private static void setLanguage(String languageCode, String languageName, Player player) {
            BugReportManager.config.set("language", languageCode);
            BugReportManager.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Language set to " + languageName + ".");
            player.openInventory(getSettingsGUI());
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();

            if (setMaxReportsClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setMaxReportsClickMap, "Set Max Reports Per Player",
                        "max-reports-per-player", (value) -> {
                            int maxReports;
                            try {
                                maxReports = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                                return;
                            }

                            BugReportManager.config.set("max-reports-per-player", maxReports);
                            BugReportManager.saveConfig();
                            player.sendMessage(ChatColor.GREEN + "Max reports per player set to " + maxReports + ".");
                        });
            } else if (setReportCooldownClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setReportCooldownClickMap, "Set Report Cooldown",
                        "report-cooldown", (value) -> {
                            int reportCooldown;
                            try {
                                reportCooldown = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                                return;
                            }

                            BugReportManager.config.set("report-cooldown", reportCooldown);
                            BugReportManager.saveConfig();
                            player.sendMessage(ChatColor.GREEN + "Report cooldown set to " + reportCooldown + " seconds.");
                        });
            } else if (setReportConfirmationMessageClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setReportConfirmationMessageClickMap, "Set Report Confirmation Message",
                        "report-confirmation-message", (value) -> {
                            BugReportManager.config.set("report-confirmation-message", value);
                            BugReportManager.saveConfig();
                            player.sendMessage(ChatColor.GREEN + "Report confirmation message set to '" + value + "' .");
                        });
            }
        }

        private void handleSettingUpdate(AsyncPlayerChatEvent event, Player player, Map<UUID, String> settingClickMap, String displayName, String configKey, Consumer<String> updateLogic) {
            String clickDisplayName = settingClickMap.get(player.getUniqueId());
            if (clickDisplayName != null && clickDisplayName.equals(ChatColor.YELLOW + displayName)) {
                event.setCancelled(true);
                if (event.getMessage().equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Cancelled.");
                    player.openInventory(getSettingsGUI());
                    settingClickMap.remove(player.getUniqueId());
                    return;
                }

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