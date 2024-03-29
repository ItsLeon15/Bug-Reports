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
import java.util.stream.IntStream;

import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.DefaultLanguageSelector.getTextElseDefault;
import static com.leon.bugreport.gui.bugreportGUI.updateBugReportItems;

public class BugReportSettings {
    public BugReportSettings() { }

    private static Integer newReportIDGUI;

    private static void setBorder(Inventory gui, Material borderMaterial) {
        IntStream.range(0, 9).forEach(i -> gui.setItem(i, createButton(borderMaterial, " ")));
        IntStream.range(36, 45).forEach(i -> gui.setItem(i, createButton(borderMaterial, " ")));
        IntStream.range(9, 36).filter(i -> i % 9 == 0 || i % 9 == 8).forEach(i -> gui.setItem(i, createButton(borderMaterial, " ")));
    }

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
        ItemStack viewAllStatus = createButton(Material.BOOKSHELF, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("viewStatus"));

        setBorder(gui, Material.GRAY_STAINED_GLASS_PANE);

        gui.setItem(10, setDiscordWebhook);
        gui.setItem(11, setBugReportNotifications);
        gui.setItem(12, toggleCategorySelection);
        gui.setItem(13, maxReportsPerPlayer);
        gui.setItem(14, setLanguage);
        gui.setItem(15, otherSettings);
        gui.setItem(16, viewAllStatus);

        gui.setItem(19, getDiscordWebhookToggle() ? onIcon : offIcon);
        gui.setItem(20, getBugReportNotificationsToggle() ? onIcon : offIcon);
        gui.setItem(21, getCategorySelectionToggle() ? onIcon : offIcon);

        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("close")));

        return gui;
    }

    static @NotNull ItemStack createButton(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean getDiscordWebhookToggle() {
        return config.getBoolean("enableDiscordWebhook");
    }

    private static void setDiscordWebhookToggle(@NotNull Player player) {
        if (BugReportManager.debugMode) plugin.getLogger().info("Discord Webhook toggle clicked by " + player.getName());
        boolean toggle = getDiscordWebhookToggle();
        config.set("enableDiscordWebhook", !toggle);
        saveConfig();
        player.getOpenInventory().setItem(19,
                getDiscordWebhookToggle() ? createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true")) :
                createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false"))
        );
    }

    private static boolean getBugReportNotificationsToggle() {
        return config.getBoolean("enableBugReportNotifications");
    }

    private static void setBugReportNotificationsToggle(@NotNull Player player) {
        if (BugReportManager.debugMode) plugin.getLogger().info("Bug Report Notifications toggle clicked by " + player.getName());
        boolean toggle = getBugReportNotificationsToggle();
        config.set("enableBugReportNotifications", !toggle);
        saveConfig();
        player.getOpenInventory().setItem(20,
                toggle ? createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false")) :
                createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true"))
        );
    }

    private static boolean getCategorySelectionToggle() {
        return config.getBoolean("enablePluginReportCategories");
    }

    private static void setCategorySelectionToggle(@NotNull Player player) {
        if (BugReportManager.debugMode) plugin.getLogger().info("Category Selection toggle clicked by " + player.getName());
        boolean toggle = getCategorySelectionToggle();
        config.set("enablePluginReportCategories", !toggle);
        saveConfig();
        player.getOpenInventory().setItem(21, toggle ?
            createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false")) :
            createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true"))
        );
    }

    private static void setLanguageToggle(@NotNull Player player) {
        if (BugReportManager.debugMode) plugin.getLogger().info("Language toggle clicked by " + player.getName());
        player.openInventory(openLanguageGUI());
    }

    private static @NotNull Inventory openLanguageGUI() {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("language"));

        setBorder(gui, Material.GRAY_STAINED_GLASS_PANE);

		gui.setItem(10, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODgzMWM3M2Y1NDY4ZTg4OGMzMDE5ZTI4NDdlNDQyZGZhYTg4ODk4ZDUwY2NmMDFmZDJmOTE0YWY1NDRkNTM2OCJ9fX0=", "English",              11));
        gui.setItem(11, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTEyNjlhMDY3ZWUzN2U2MzYzNWNhMWU3MjNiNjc2ZjEzOWRjMmRiZGRmZjk2YmJmZWY5OWQ4YjM1Yzk5NmJjIn19fQ==", "French",               12));
        gui.setItem(12, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ==", "German",               13));
        gui.setItem(13, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzJkNzMwYjZkZGExNmI1ODQ3ODNiNjNkMDgyYTgwMDQ5YjVmYTcwMjI4YWJhNGFlODg0YzJjMWZjMGMzYThiYyJ9fX0=", "Spanish",              14));
        gui.setItem(14, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODVjZTg5MjIzZmE0MmZlMDZhZDY1ZDhkNDRjYTQxMmFlODk5YzgzMTMwOWQ2ODkyNGRmZTBkMTQyZmRiZWVhNCJ9fX0=", "Italian",              15));
        gui.setItem(15, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2Y5YmMwMzVjZGM4MGYxYWI1ZTExOThmMjlmM2FkM2ZkZDJiNDJkOWE2OWFlYjY0ZGU5OTA2ODE4MDBiOThkYyJ9fX0=", "Simplified Chinese",   16));
        gui.setItem(16, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTZlYWZlZjk4MGQ2MTE3ZGFiZTg5ODJhYzRiNDUwOTg4N2UyYzQ2MjFmNmE4ZmU1YzliNzM1YTgzZDc3NWFkIn19fQ==", "Russian",              17));

        String language = config.getString("language");

        for (int i = 19; i < 26; i++) {
            gui.setItem(i, createButton(Material.GRAY_DYE, BugReportLanguage.getTitleFromLanguage("false")));
        }

        switch (Objects.requireNonNull(language)) {
            case "en" -> gui.setItem(28-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
            case "fr" -> gui.setItem(29-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
            case "de" -> gui.setItem(30-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
            case "es" -> gui.setItem(31-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
            case "it" -> gui.setItem(32-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
            case "zh" -> gui.setItem(33-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
            case "ru" -> gui.setItem(34-9, createButton(Material.LIME_DYE, BugReportLanguage.getTitleFromLanguage("true")));
        }

        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));

        return gui;
    }

    public static @NotNull ItemStack createCustomPlayerHead(String texture, String name, int modelData) {
        if (BugReportManager.debugMode) plugin.getLogger().info("Creating custom player head with texture: " + texture + ", name: " + name + ", modelData: " + modelData);
        return createCustomPlayerHead(texture, name, modelData, null);
    }

    public static @NotNull ItemStack createCustomPlayerHead(String texture, String name, int modelData, ChatColor nameColor) {
        if (BugReportManager.debugMode) plugin.getLogger().info("Creating custom player head with texture: " + texture + ", name: " + name + ", modelData: " + modelData + ", nameColor: " + nameColor);
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        if (skullMeta != null) {
            try {
                String decodedValue = new String(Base64.getDecoder().decode(texture));
                JsonObject textureJson = JsonParser.parseString(decodedValue).getAsJsonObject();
                String textureUrl = textureJson.getAsJsonObject("textures")
                        .getAsJsonObject("SKIN")
                        .get("url").getAsString();

                if (BugReportManager.debugMode) plugin.getLogger().info("Texture URL: " + textureUrl);

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(textureUrl));
                profile.setTextures(textures);

                skullMeta.setOwnerProfile(profile);
                skullMeta.setDisplayName((nameColor != null ? nameColor : ChatColor.YELLOW) + name);
                skullMeta.setCustomModelData(modelData);
                playerHead.setItemMeta(skullMeta);

                if (BugReportManager.debugMode) plugin.getLogger().info("Custom player head created successfully.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create custom player head: " + e.getMessage());
                return new ItemStack(Material.PLAYER_HEAD);
            }
        }

        return playerHead;
    }

    public static @NotNull Inventory getStatusSelectionGUI(Integer reportIDGUI) {
        newReportIDGUI = reportIDGUI;
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("statusSelection"));

        setBorder(gui, Material.GRAY_STAINED_GLASS_PANE);

        List<Map<?, ?>> statuses = config.getMapList("statuses");
        for (Map<?, ?> statusMap : statuses) {
            String statusName = (String) statusMap.get("name");
            String statusDescription = (String) statusMap.get("description");

            ChatColor newStatusColor = ChatColor.valueOf((String) statusMap.get("color")) != null ?
                    ChatColor.valueOf((String) statusMap.get("color")) :
                    ChatColor.WHITE;

            Material newStatusIcon = Material.matchMaterial((String) statusMap.get("icon")) != null ?
                    Material.matchMaterial((String) statusMap.get("icon")) :
                    Material.BARRIER;

            ItemStack statusItem = createButton(newStatusIcon,newStatusColor + statusName);
            ItemMeta statusItemMeta = statusItem.getItemMeta();
            if (statusItemMeta != null) {
                statusItemMeta.setLore(Collections.singletonList(ChatColor.GRAY + statusDescription));
                statusItem.setItemMeta(statusItemMeta);
            }

            gui.addItem(statusItem);
        }

        gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("close")));

        return gui;
    }

    public static class BugReportSettingsListener implements Listener {
        private final Map<UUID, String> setMaxReportsClickMap = new HashMap<>();
        private final Map<UUID, String> setReportCooldownClickMap = new HashMap<>();

        private final Map<UUID, String> addStatusClickMap = new HashMap<>();
        private final Map<UUID, String> removeStatusClickMap = new HashMap<>();
        private final Map<UUID, String> renameStatusClickMap = new HashMap<>();
        private final Map<UUID, String> setNewMaterialStatusClickMap = new HashMap<>();
        private final Map<UUID, String> setNewColorStatusClickMap = new HashMap<>();
        private final Map<UUID, String> setNewDescriptionStatusClickMap = new HashMap<>();

        private String savedStatusName = "";
        private Integer savedStatusID = 0;

        @EventHandler(priority = EventPriority.NORMAL)
        public void onInventoryClick(@NotNull InventoryClickEvent event) {
            String displayName = ChatColor.stripColor(event.getView().getTitle());

            if (displayName.contains("Bug Report - ")) {
                displayName = displayName.substring(13);
            }

            String customDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(displayName);

            if (customDisplayName.contains("Status Selection")) {
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
                String customItemDisplayName = ChatColor.stripColor(itemDisplayName);

                if (customItemDisplayName.equals("Close")) {
                    player.closeInventory();
                    return;
                }

                List<Map<?, ?>> statuses = config.getMapList("statuses");
                for (Map<?, ?> statusMap : statuses) {
                    String statusName = (String) statusMap.get("name");
                    if (statusName.equals(customItemDisplayName)) {
                        Integer statusID = (Integer) statusMap.get("id");
                        BugReportDatabase.updateReportStatus(newReportIDGUI, statusID);
                        player.closeInventory();
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + "The status of the report has been updated to " + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ".");
                    }
                }
            }

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
                        if (config.getBoolean("useTitleInsteadOfMessage")) {
                            player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(language, "enterMaxReportsPerPlayer"), 10, 70, 20);
                        } else {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + getTextElseDefault(language, "enterMaxReportsPerPlayer"));
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
                    case "View Status" -> player.openInventory(getViewStatusGUI());
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
                    case "Enable Report Book" -> setReportBook(player);
                }
            }

            if (customDisplayName.contains("View Status")) {
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

                if (customItemDisplayName.equals(" ")) {
                    return;
                }

                if (customItemDisplayName.equals("Add Status")) {
                    player.closeInventory();
                    List<Map<?, ?>> statuses = config.getMapList("statuses");
                    if (statuses.size() >= 9) {
                        sendMessageOrTitle(player, "statusLimitReached", new HashMap<>());
                        return;
                    }

                    player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW + "Type the name for the new status or 'cancel' to cancel.");
                    addStatusClickMap.put(player.getUniqueId(), String.valueOf(true));
                }

                List<Map<?, ?>> statuses = config.getMapList("statuses");
                for (Map<?, ?> statusMap : statuses) {
                    String statusName = (String) statusMap.get("name");
                    Integer statusID = (Integer) statusMap.get("id");

                    if (statusName.equals(customItemDisplayName)) {
                        savedStatusName = statusName;
                        savedStatusID = statusID;

                        player.openInventory(getStatusInfoGUI(statusMap));
                    }
                }
            }

            if (customDisplayName.contains("Edit Status")) {
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
                    player.openInventory(getViewStatusGUI());
                    return;
                }

                if (customItemDisplayName.equals(" ")) {
                    return;
                }

                if (customItemDisplayName.equals("Delete")) {
                    List<Map<?, ?>> statuses = config.getMapList("statuses");
                    for (Map<?, ?> statusMap : statuses) {
                        String statusName = (String) statusMap.get("name");
                        if (statusMap.get("id").equals(savedStatusID)) {
                            player.closeInventory();
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW +
                                    "Type 'confirm' to delete the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel."
                            );

                            removeStatusClickMap.put(player.getUniqueId(), String.valueOf(true));
                            removeStatusClickMap.put(player.getUniqueId(), statusName);
                        }
                    }
                }

                if (customItemDisplayName.equals("Status Name")) {
                    List<Map<?, ?>> statuses = config.getMapList("statuses");
                    for (Map<?, ?> statusMap : statuses) {
                        String statusName = (String) statusMap.get("name");
                        if (statusMap.get("id").equals(savedStatusID)) {
                            player.closeInventory();
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW +
                                    "Type the new name for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel."
                            );

                            renameStatusClickMap.put(player.getUniqueId(), String.valueOf(true));
                            renameStatusClickMap.put(player.getUniqueId(), statusName);
                        }
                    }
                }

                if (customItemDisplayName.equals("Status Material")) {
                    List<Map<?, ?>> statuses = config.getMapList("statuses");
                    for (Map<?, ?> statusMap : statuses) {
                        String statusName = (String) statusMap.get("name");
                        if (statusMap.get("id").equals(savedStatusID)) {
                            player.closeInventory();
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW +
                                    "Type the new material for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel."
                            );

                            setNewMaterialStatusClickMap.put(player.getUniqueId(), String.valueOf(true));
                            setNewMaterialStatusClickMap.put(player.getUniqueId(), statusName);
                        }
                    }
                }

                if (customItemDisplayName.equals("Status Color")) {
                    List<Map<?, ?>> statuses = config.getMapList("statuses");
                    for (Map<?, ?> statusMap : statuses) {
                        String statusName = (String) statusMap.get("name");
                        if (statusMap.get("id").equals(savedStatusID)) {
                            player.closeInventory();
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW +
                                    "Type the new color for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel."
                            );

                            setNewColorStatusClickMap.put(player.getUniqueId(), String.valueOf(true));
                            setNewColorStatusClickMap.put(player.getUniqueId(), statusName);
                        }
                    }
                }

                if (customItemDisplayName.equals("Status Description")) {
                    List<Map<?, ?>> statuses = config.getMapList("statuses");
                    for (Map<?, ?> statusMap : statuses) {
                        String statusName = (String) statusMap.get("name");

                        if (statusMap.get("id").equals(savedStatusID)) {
                            player.closeInventory();
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.YELLOW +
                                    "Type the new description for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel."
                            );

                            setNewDescriptionStatusClickMap.put(player.getUniqueId(), String.valueOf(true));
                            setNewDescriptionStatusClickMap.put(player.getUniqueId(), statusName);
                        }
                    }
                }
            }
        }

        private @NotNull Inventory getViewStatusGUI() {
            Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("viewStatus"));

            setBorder(gui, Material.GRAY_STAINED_GLASS_PANE);

            List<Map<?, ?>> statuses = config.getMapList("statuses");

            int statusIndex = 0;
            for (int i = 9; i < 36; i++) {
                if (i % 9 != 0 && i % 9 != 8) {
                    if (statusIndex < statuses.size()) {
                        Map<?, ?> statusMap = statuses.get(statusIndex);
                        String statusName = (String) statusMap.get("name");

                        ChatColor statusColor = ChatColor.valueOf((String) statusMap.get("color")) != null ?
                            ChatColor.valueOf((String) statusMap.get("color")) :
                            ChatColor.WHITE;

                        Material statusIcon = Material.matchMaterial((String) statusMap.get("icon")) != null ?
                            Material.matchMaterial((String) statusMap.get("icon")) :
                            Material.BARRIER;

                        ItemStack statusItem = createButton(statusIcon, statusColor + statusName);

                        ItemMeta statusMeta = statusItem.getItemMeta();
                        List<String> statusLore = new ArrayList<>();
                        statusLore.add(ChatColor.GRAY + "Click to edit the status (" + statusColor + statusName + ChatColor.GRAY + ")");
                        Objects.requireNonNull(statusMeta).setLore(statusLore);
                        statusItem.setItemMeta(statusMeta);

                        gui.setItem(i, statusItem);
                        statusIndex++;
                    }

                }
            }

            gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));
            gui.setItem(42, createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzOGQwOGJkMDQwNWMwNWY0N2VhODZkNjY2NDM0MzRmZGQyZThjNDZmZjFlNmY4ODJiYjliZjg5MWM3ZDNhNSJ9fX0=", BugReportLanguage.getTitleFromLanguage("addStatus"), 1));

            return gui;
        }

        private @NotNull Inventory getStatusInfoGUI(@NotNull Map<?, ?> statusMap) {
            Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Bug Report - " + BugReportLanguage.getTitleFromLanguage("editStatus"));

            Material itemStackMaterial = getMaterialFromMap(statusMap, "icon", Material.BARRIER);
            ChatColor itemStackColor = getChatColorFromMap(statusMap, "color", ChatColor.WHITE);

            setBorder(gui, getStainedGlassPaneColor(statusMap));

            ItemStack statusName = createButton(Material.NAME_TAG, itemStackColor + BugReportLanguage.getTitleFromLanguage("statusName"));
            ItemStack statusMaterial = createButton(itemStackMaterial, itemStackColor + BugReportLanguage.getTitleFromLanguage("statusMaterial"));
            ItemStack statusColor = createButton(Material.PAINTING, itemStackColor + BugReportLanguage.getTitleFromLanguage("statusColor"));
            ItemStack statusDescription = createButton(Material.BOOK, itemStackColor + BugReportLanguage.getTitleFromLanguage("statusDescription"));
            ItemStack deleteStatus = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmUwZmQxMDE5OWU4ZTRmY2RhYmNhZTRmODVjODU5MTgxMjdhN2M1NTUzYWQyMzVmMDFjNTZkMThiYjk0NzBkMyJ9fX0=", BugReportLanguage.getTitleFromLanguage("delete"), 1);

            setItemMeta(statusName, statusMap, "name");
            setItemMeta(statusMaterial, statusMap, "icon");
            setItemMeta(statusColor, statusMap, "color");
            setItemMeta(statusDescription, statusMap, "description");

            gui.setItem(20, statusName);
            gui.setItem(21, statusColor);
            gui.setItem(22, statusDescription);
            gui.setItem(23, statusMaterial);
            gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));
            gui.setItem(42, deleteStatus);

            return gui;
        }

        private Material getMaterialFromMap(@NotNull Map<?, ?> map, String key, Material defaultMaterial) {
            Object value = map.get(key);
            return value != null ? Material.matchMaterial((String) value) : defaultMaterial;
        }

        private ChatColor getChatColorFromMap(@NotNull Map<?, ?> map, String key, ChatColor defaultColor) {
            Object value = map.get(key);
            return value != null ? ChatColor.valueOf((String) value) : defaultColor;
        }

        private Material getStainedGlassPaneColor(@NotNull Map<?, ?> map) {
            String color = map.get("color").toString().toUpperCase() + "_STAINED_GLASS_PANE";
            return Material.matchMaterial(color) != null ? Material.valueOf(color) : Material.GRAY_STAINED_GLASS_PANE;
        }

        private void setItemMeta(@NotNull ItemStack item, @NotNull Map<?, ?> map, String key) {
            ItemMeta meta = item.getItemMeta();
            Objects.requireNonNull(meta).setLore(Collections.singletonList(ChatColor.GRAY + (String) map.get(key)));
            item.setItemMeta(meta);
        }

        private @NotNull Inventory getOtherSettingsGUI() {
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
            ItemStack enableReportBook = createButton(Material.WRITABLE_BOOK, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("enableReportBook"));
            ItemStack onIcon = createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true"));
            ItemStack offIcon = createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false"));

            gui.setItem(10, enableTitleMessage);
            gui.setItem(11, enablePlayerHeads);
            gui.setItem(12, enableReportBook);

            gui.setItem(19, getTitleMessage() ? onIcon : offIcon);
            gui.setItem(20, getPlayerHead() ? onIcon : offIcon);
            gui.setItem(21, getReportBook() ? onIcon : offIcon);

            gui.setItem(40, createButton(Material.BARRIER, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("back")));

            return gui;
        }

        private boolean getTitleMessage() {
            return config.getBoolean("useTitleInsteadOfMessage");
        }

        private void setTitleMessage(@NotNull Player player) {
            boolean toggle = getTitleMessage();
            config.set("useTitleInsteadOfMessage", !toggle);
            saveConfig();
            if (BugReportManager.debugMode) plugin.getLogger().info("Title message set to " + !toggle);
            player.getOpenInventory().setItem(19, getTitleMessage() ?
                createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true")) :
                createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false"))
            );
        }

        private boolean getPlayerHead() {
            return config.getBoolean("enablePlayerHeads");
        }

        private void setPlayerHead(@NotNull Player player) {
            boolean toggle = getPlayerHead();
            config.set("enablePlayerHeads", !toggle);
            saveConfig();
            if (BugReportManager.debugMode) plugin.getLogger().info("Player heads set to " + !toggle);
            player.getOpenInventory().setItem(20, getPlayerHead() ?
                createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true")) :
                createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false"))
            );
        }

        private boolean getReportBook() {
            return config.getBoolean("enablePluginReportBook");
        }

        private void setReportBook(@NotNull Player player) {
            boolean toggle = getReportBook();
            config.set("enablePluginReportBook", !toggle);
            saveConfig();
            if (BugReportManager.debugMode) plugin.getLogger().info("Report book set to " + !toggle);
            player.getOpenInventory().setItem(21, getReportBook() ?
                    createButton(Material.LIME_DYE, ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("true")) :
                    createButton(Material.GRAY_DYE, ChatColor.RED + BugReportLanguage.getTitleFromLanguage("false"))
            );
        }

        private static void setLanguage(String languageCode, String languageName, @NotNull Player player) {
            player.closeInventory();
            if (checkForKey("useTitleInsteadOfMessage", true)) {
                player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(languageCode, "languageSetTo").replace("%language%", languageName), 10, 70, 20);
            } else {
                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + getTextElseDefault(languageCode, "languageSetTo").replace("%language%", languageName));
            }
            updateBugReportItems();
            config.set("language", languageCode);
            if (BugReportManager.debugMode) plugin.getLogger().info("Language set to " + languageCode);
            saveConfig();
            loadConfig();

            player.openInventory(openLanguageGUI());
        }

        private void handleCancel(Player player, @NotNull Map<UUID, String> clickMap) {
            sendMessageOrTitle(player, "cancelled", new HashMap<>());
            clickMap.remove(player.getUniqueId());
        }

        private void sendMessageOrTitle(Player player, String key, @NotNull Map<String, String> replacements) {
            String message = getTextElseDefault(config.getString("language"), key);

            if (!replacements.isEmpty()) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    message = message.replace(entry.getKey(), entry.getValue());
                }
            }

            if (checkForKey("useTitleInsteadOfMessage", true)) {
                player.sendTitle(pluginColor + pluginTitle, message, 10, 70, 20);
            } else {
                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + message);
            }
        }


        private @NotNull List<Map<String, Object>> getTypedStatusList() {
            List<Map<?, ?>> originalStatuses = config.getMapList("statuses");
            List<Map<String, Object>> statuses = new ArrayList<>();
            for (Map<?, ?> originalMap : originalStatuses) {
                Map<String, Object> typedMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        typedMap.put((String) entry.getKey(), entry.getValue());
                    }
                }
                statuses.add(typedMap);
            }
            return statuses;
        }

        private void updateStatus(@NotNull List<Map<String, Object>> statuses, Player player, Map<UUID, String> clickMap, String updateKey, Object newValue) {
            statuses.forEach(statusMap -> {
                if (statusMap.get("name").equals(clickMap.get(player.getUniqueId())) && statusMap.get("id").equals(savedStatusID)) {
                    statusMap.put(updateKey, newValue);
                    config.set("statuses", statuses);
                    saveConfig();
                }
            });
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
                                player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(config.getString("language"), "enterValidNumber"), 10, 70, 20);
                            } else {
                                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + getTextElseDefault(config.getString("language"), "enterValidNumber"));
                            }
                            return;
                        }
                        config.set("max-reports-per-player", maxReports);
                        saveConfig();
                        if (checkForKey("useTitleInsteadOfMessage", true)) {
                            player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(config.getString("language"), "maxReportsPerPlayerSuccessMessage")
                                    .replace("%amount%", String.valueOf(maxReports)), 10, 70, 20);
                        } else {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + getTextElseDefault(config.getString("language"), "maxReportsPerPlayerSuccessMessage")
                                    .replace("%amount%", String.valueOf(maxReports)));
                        }
                    } else {
                        value = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
                        String customDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(value);
                        if (customDisplayName.contains("Cancel")) {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + BugReportLanguage.getTitleFromLanguage("cancelled"));
                        } else {
                            if (checkForKey("useTitleInsteadOfMessage", true)) {
                                player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(config.getString("language"), "enterValidNumber"), 10, 70, 20);
                            } else {
                                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + getTextElseDefault(config.getString("language"), "enterValidNumber"));
                            }
                        }
                    }
                });
            }

            if (setReportCooldownClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setReportCooldownClickMap, "Set Report Cooldown", "report-cooldown", (value) -> {
                    int reportCooldown;
                    try {
                        reportCooldown = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        if (checkForKey("useTitleInsteadOfMessage", true)) {
                            player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(config.getString("language"), "enterValidNumber"), 10, 70, 20);
                        } else {
                            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + getTextElseDefault(config.getString("language"), "enterValidNumber"));
                        }
                        return;
                    }

                    config.set("report-cooldown", reportCooldown);
                    saveConfig();
                    if (checkForKey("useTitleInsteadOfMessage", true)) {
                        player.sendTitle(pluginColor + pluginTitle, getTextElseDefault(config.getString("language"), "reportCooldownSuccessMessage")
                            .replace("%time%", String.valueOf(reportCooldown)), 10, 70, 20);
                    } else {
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + getTextElseDefault(config.getString("language"), "reportCooldownSuccessMessage")
                                .replace("%time%", String.valueOf(reportCooldown)));
                    }
                });
            }

            if (removeStatusClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, removeStatusClickMap, savedStatusName, "remove-status", (value) -> {
                    if (event.getMessage().equalsIgnoreCase("cancel")) {
                        handleCancel(player, removeStatusClickMap);
                        return;
                    }
                    if (value.equalsIgnoreCase("confirm")) {
                        List<Map<String, Object>> statuses = getTypedStatusList();
                        statuses.removeIf(statusMap -> statusMap.get("name").equals(removeStatusClickMap.get(player.getUniqueId())) && statusMap.get("id").equals(savedStatusID));
                        config.set("statuses", statuses);
                        saveConfig();

                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("%status%", savedStatusName);
                        sendMessageOrTitle(player, "statusRemoved", replacements);
                    }
                });
            }

            if (renameStatusClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, renameStatusClickMap, savedStatusName, "rename-status", (value) -> {
                    if (event.getMessage().equalsIgnoreCase("cancel")) {
                        handleCancel(player, renameStatusClickMap);
                        return;
                    }
                    List<Map<String, Object>> statuses = getTypedStatusList();
                    updateStatus(statuses, player, renameStatusClickMap, "name", value);

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%status%", value);
                    sendMessageOrTitle(player, "statusRenamed", replacements);
                });
            }

            if (setNewMaterialStatusClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setNewMaterialStatusClickMap, savedStatusName, "material-status", (value) -> {
                    if (event.getMessage().equalsIgnoreCase("cancel")) {
                        handleCancel(player, setNewMaterialStatusClickMap);
                        return;
                    }
                    List<Map<String, Object>> statuses = getTypedStatusList();
                    updateStatus(statuses, player, setNewMaterialStatusClickMap, "icon", value);

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%material%", value);
                    sendMessageOrTitle(player, "statusMaterialChanged", replacements);
                });
            }

            if (setNewColorStatusClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setNewColorStatusClickMap, savedStatusName, "color-status", (value) -> {
                    if (event.getMessage().equalsIgnoreCase("cancel")) {
                        handleCancel(player, setNewColorStatusClickMap);
                        return;
                    }
                    List<Map<String, Object>> statuses = getTypedStatusList();
                    updateStatus(statuses, player, setNewColorStatusClickMap, "color", value);

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%color%", value);
                    sendMessageOrTitle(player, "statusColorChanged", replacements);
                });
            }

            if (setNewDescriptionStatusClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, setNewDescriptionStatusClickMap, savedStatusName, "description-status", (value) -> {
                    if (event.getMessage().equalsIgnoreCase("cancel")) {
                        handleCancel(player, setNewDescriptionStatusClickMap);
                        return;
                    }
                    List<Map<String, Object>> statuses = getTypedStatusList();
                    updateStatus(statuses, player, setNewDescriptionStatusClickMap, "description", value);

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%description%", value);
                    sendMessageOrTitle(player, "statusDescriptionChanged", replacements);
                });
            }

            if (addStatusClickMap.containsKey(player.getUniqueId())) {
                handleSettingUpdate(event, player, addStatusClickMap, BugReportLanguage.getTitleFromLanguage("addStatus"), "add-status", (value) -> {
                    if (event.getMessage().equalsIgnoreCase("cancel")) {
                        handleCancel(player, addStatusClickMap);
                        return;
                    }
                    List<Map<String, Object>> statuses = getTypedStatusList();

                    Map<String, Object> newStatus = new HashMap<>();

                    newStatus.put("name", value);
                    newStatus.put("id", statuses.size() + 1);
                    newStatus.put("icon", "BARRIER");
                    newStatus.put("color", "WHITE");
                    newStatus.put("description", " ");

                    statuses.add(newStatus);
                    config.set("statuses", statuses);
                    saveConfig();

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%status%", value);
                    sendMessageOrTitle(player, "statusAdded", replacements);
                });
            }
		}

        private void handleSettingUpdate(
                AsyncPlayerChatEvent event,
                @NotNull Player player,
                @NotNull Map<UUID, String> settingClickMap,
                String displayName,
                String ignoredConfigKey,
                Consumer<String> updateLogic
        ) {
            String clickDisplayName = settingClickMap.get(player.getUniqueId());
            String displayNameDefault = BugReportLanguage.getEnglishVersionFromLanguage(displayName);

            if (clickDisplayName != null && clickDisplayName.equals(displayNameDefault)) {
                event.setCancelled(true);

                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.runTask(plugin, () -> {
                    updateLogic.accept(event.getMessage());
                    player.openInventory(getSettingsGUI());
                    settingClickMap.remove(player.getUniqueId());
                });
            }
        }
    }
}