package com.leon.bugreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class BugReportManager {

    static Map<UUID, List<String>> bugReports;
    private static BugReportDatabase database;
    static Plugin plugin;
    public static FileConfiguration config;
    private File configFile;
    private final LinkDiscord discord;
    private final List<Category> reportCategories;

    public BugReportManager(Plugin plugin, String dbFilePath) {
        BugReportManager.plugin = plugin;
        bugReports = new HashMap<>();
        database = new BugReportDatabase(dbFilePath);

        loadBugReports();
        loadConfig();

        String webhookURL = config.getString("webhookURL", "");
        discord = new LinkDiscord(webhookURL);

        reportCategories = loadReportCategories();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private List<Category> loadReportCategories() {
        List<Category> categories = new ArrayList<>();

        List<Map<?, ?>> categoryList = config.getMapList("reportCategories");

        for (Map<?, ?> categoryMap : categoryList) {
            String name = categoryMap.get("name").toString();
            int id = Integer.parseInt(categoryMap.get("id").toString());
            String description = categoryMap.get("description").toString();
            String itemString = categoryMap.get("item").toString();

            Material itemMaterial = Material.matchMaterial(itemString);
            if (itemMaterial == null) {
                continue;
            }

            ItemStack itemStack = new ItemStack(itemMaterial);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatColor.YELLOW + name);
            itemMeta.setLore(Collections.singletonList(ChatColor.GRAY + description));
            itemStack.setItemMeta(itemMeta);
            categories.add(new Category(id, name, itemStack));
        }

        return categories;
    }

    public List<Category> getReportCategories() {
        return reportCategories;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setWebhookURL(String webhookURL) {
        config.set("webhookURL", webhookURL);
        saveConfig();
        discord.setWebhookURL(webhookURL);
    }

    public void submitBugReport(Player player, String message, Integer categoryId) {
        UUID playerId = player.getUniqueId();
        List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

        String playerName = player.getName();
        String playerUUID = playerId.toString();
        String worldName = player.getWorld().getName();

        String header = "Username: " + playerName + "\n" +
                "UUID: " + playerUUID + "\n" +
                "World: " + worldName + "\n" +
                "hasBeenRead: 0" + "\n" +
                "Category ID: " + categoryId + "\n" +
                "Full Message: " + message;

        reports.add(header);
        bugReports.put(playerId, reports);

        database.addBugReport(playerName, playerId, worldName, header, message);

        if (!config.getString("webhookURL", "").isEmpty()) {
            try {
                discord.sendBugReport(message, playerId, worldName, playerName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static Inventory getBugReportGUI(Player player) {
        int itemsPerPage = 27;
        int navigationRow = 36;

        UUID playerId = player.getUniqueId();
        List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

        int totalPages = (int) Math.ceil((double) reports.size() / itemsPerPage);
        int currentPage = Math.max(1, Math.min(getCurrentPage(player), totalPages));

        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.RESET + "Bug Reports - Page " + currentPage);

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, reports.size());
        for (int i = startIndex; i < endIndex; i++) {
            String report = reports.get(i);
            String firstLine = report.split("\n")[0];

            ItemStack reportItem = null;
            if (report.contains("hasBeenRead: 0")) {
                reportItem = new ItemStack(Material.ENCHANTED_BOOK);
            } else if (report.contains("hasBeenRead: 1")) {
                reportItem = new ItemStack(Material.BOOK);
            } else {
                reportItem = new ItemStack(Material.BOOK);
            }

            ItemMeta itemMeta = reportItem.getItemMeta();
            itemMeta.setDisplayName(ChatColor.YELLOW + "Bug Report #" + (i + 1));
            itemMeta.setLore(Collections.singletonList(ChatColor.GRAY + firstLine));
            reportItem.setItemMeta(itemMeta);

            gui.setItem(i - startIndex, reportItem);
        }

        ItemStack backButton = createButton(Material.ARROW, ChatColor.GREEN + "Back");
        ItemStack forwardButton = createButton(Material.ARROW, ChatColor.GREEN + "Forward");
        ItemStack pageIndicator = createButton(Material.PAPER,
                ChatColor.YELLOW + "Page " + currentPage + " of " + totalPages);

        if (currentPage > 1) {
            gui.setItem(navigationRow, backButton);
        }
        if (currentPage < totalPages) {
            gui.setItem(navigationRow + 8, forwardButton);
        }

        gui.setItem(navigationRow + 4, pageIndicator);

        return gui;
    }

    private static ItemStack createButton(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private void loadBugReports() {
        Map<UUID, List<String>> loadedReports = database.loadBugReports();
        if (loadedReports != null) {
            bugReports = loadedReports;
        }
    }

    public static class BugReportListener implements Listener {

        private final BugReportManager reportManager;
        private final Map<UUID, Boolean> closingInventoryMap;

        public BugReportListener(BugReportManager reportManager) {
            this.reportManager = reportManager;
            this.closingInventoryMap = new HashMap<>();
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onInventoryClick(InventoryClickEvent event) {
            if (!event.getView().getTitle().startsWith(ChatColor.RESET + "Bug Reports")) {
                return;
            }

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

            String displayName = itemMeta.getDisplayName();
            if (displayName.equals(ChatColor.GREEN + "Back")) {
                int currentPage = getCurrentPage(player);
                if (currentPage > 1) {
                    setCurrentPage(player, currentPage - 1);
                    player.openInventory(getBugReportGUI(player));
                }
            } else if (displayName.equals(ChatColor.GREEN + "Forward")) {
                int currentPage = getCurrentPage(player);
                if (currentPage < reportManager.getTotalPages(player)) {
                    setCurrentPage(player, currentPage + 1);
                    player.openInventory(getBugReportGUI(player));
                }
            } else if (displayName.startsWith(ChatColor.YELLOW + "Bug Report #")) {
                int itemSlot = event.getSlot();
                int startIndex = (getCurrentPage(player) - 1) * 27;
                int reportIndex = startIndex + itemSlot;
                UUID playerId = player.getUniqueId();
                List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

                System.out.println("---------------------");
                System.out.println(reports);
                System.out.println("---------------------");

                String report = reports.get(reportIndex);
                if (report.contains("hasBeenRead: 0")) {
                    report = report.replace("hasBeenRead: 0", "hasBeenRead: 1");
                    reports.set(reportIndex, report);
                    bugReports.put(playerId, reports);
                    database.updateBugReportHeader(playerId, reportIndex, 1);
                }
                if (reportIndex >= 0 && reportIndex < reports.size()) {
                    reports.set(reportIndex, report);
                    openBugReportDetailsGUI(player, report, reportIndex + 1);
                }
            }
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onInventoryClose(InventoryCloseEvent event) {
            if (event.getView().getTitle().startsWith(ChatColor.RESET + "Bug Reports")) {
                Player player = (Player) event.getPlayer();
                UUID playerId = player.getUniqueId();

                if (closingInventoryMap.getOrDefault(playerId, false)) {
                    closingInventoryMap.put(playerId, false);
                    return;
                }

                closingInventoryMap.remove(playerId);
            }
        }
    }

    public static int getCurrentPage(Player player) {
        return player.getMetadata("currentPage").get(0).asInt();
    }

    public int getTotalPages(Player player) {
        UUID playerId = player.getUniqueId();
        List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());
        int totalPages = (int) Math.ceil((double) reports.size() / 27);
        return totalPages;
    }

    public static void setCurrentPage(Player player, int page) {
        player.setMetadata("currentPage", new FixedMetadataValue(plugin, page));
    }

    private static void openBugReportDetailsGUI(Player player, String report, Integer ID) {
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.YELLOW + "Bug Report Details - #" + ID);

        System.out.println(report);

        String[] reportLines = report.split("\n");

        String username = "";
        String uuid = "";
        String world = "";
        String fullMessage = "";
        String category = "";
        ItemStack categoryItem = null;
        boolean longMessage = false;

        for (String line : reportLines) {
            String trimmed = line.substring(line.indexOf(":") + 1).trim();

            System.out.println("______________________");
            System.out.println(line);
            System.out.println("______________________");

            if (line.startsWith("Username:")) {
                username = trimmed;
            } else if (line.startsWith("UUID:")) {
                uuid = trimmed;
            } else if (line.startsWith("World:")) {
                world = trimmed;
            } else if (line.startsWith("Full Message:")) {
                fullMessage = trimmed;
            } else if (line.startsWith("Category ID:")) {
                category = trimmed;
            }
        }

        longMessage = fullMessage.length() > 32;

        ItemStack emptyItem = createEmptyItem();
        ItemStack usernameItem = createInfoItem(Material.PLAYER_HEAD, ChatColor.GOLD + "Username", ChatColor.WHITE + username);
        ItemStack uuidItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "UUID", ChatColor.WHITE + uuid);
        ItemStack worldItem = createInfoItem(Material.GRASS_BLOCK, ChatColor.GOLD + "World", ChatColor.WHITE + world);
        ItemStack messageItem = createInfoItem(Material.PAPER, ChatColor.GOLD + "Full Message", ChatColor.WHITE + fullMessage, longMessage);


        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, emptyItem);
        }

        gui.setItem(10, usernameItem);
        gui.setItem(12, uuidItem);
        gui.setItem(14, worldItem);
        gui.setItem(16, messageItem);

        if (!category.equals("null") && !category.equals("")) {
            String categoryName = "";
            List<Map<?, ?>> categoryList = config.getMapList("reportCategories");

            for (Map<?, ?> categoryMap : categoryList) {
                String name = categoryMap.get("name").toString();
                int id = Integer.parseInt(categoryMap.get("id").toString());

                if (id == Integer.parseInt(category)) {
                    categoryName = name;
                    break;
                }
            }

            categoryItem = createInfoItem(Material.CHEST, ChatColor.GOLD + "Category Name", ChatColor.WHITE + categoryName, false);
            gui.setItem(22, categoryItem);
        }

        player.openInventory(gui);

        Bukkit.getPluginManager().registerEvents(new BugReportDetailsListener(gui), plugin);
    }

    private record BugReportDetailsListener(Inventory gui) implements Listener {
        @EventHandler(priority = EventPriority.NORMAL)
        public void onInventoryClick(InventoryClickEvent event) {
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory != null && clickedInventory.equals(gui)) {
                event.setCancelled(true);
            }
        }
    }

    private static ItemStack createEmptyItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createInfoItem(Material material, String name, String value, Boolean... longMessage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (longMessage.length > 0 && longMessage[0]) {
            List<String> lore = new ArrayList<>();
            String[] words = value.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                if (currentLine.length() + word.length() > 30) {
                    lore.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }

                currentLine.append(word).append(" ");
            }

            if (currentLine.length() > 0) {
                lore.add(currentLine.toString());
            }

            meta.setLore(lore);
        } else {
            meta.setLore(Collections.singletonList(value));
        }

        item.setItemMeta(meta);

        return item;
    }
}