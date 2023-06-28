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
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class BugReportManager {

    private static Map<UUID, List<String>> bugReports;
    private final BugReportDatabase database;
    private static Plugin plugin;

    private final FileConfiguration config;
    private final File configFile;
    private final LinkDiscord discord;

    public BugReportManager(Plugin plugin, String dbFilePath) {
        BugReportManager.plugin = plugin;
        bugReports = new HashMap<>();
        database = new BugReportDatabase(dbFilePath);

        loadBugReports();

        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        config.options().copyDefaults(true);

        String webhookURL = config.getString("webhookURL", "");
        discord = new LinkDiscord(webhookURL);
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

    public void submitBugReport(Player player, String message) {
        UUID playerId = player.getUniqueId();
        List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

        String playerName = player.getName();
        String playerUUID = playerId.toString();
        String worldName = player.getWorld().getName();

        String formattedReport = "Username: " + playerName + "\n" +
                "UUID: " + playerUUID + "\n" +
                "World: " + worldName + "\n" +
                "Full Message: " + message;

        reports.add(formattedReport);
        bugReports.put(playerId, reports);

        String header = "Username: " + playerName + "\nUUID: " + playerUUID + "\nWorld: " + worldName;

        database.addBugReport(playerName, playerId, worldName, header, message);

        try {
            discord.sendBugReport(message, playerId, worldName, playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int currentPage = 1;
    private int totalPages = 1;

    public Inventory getBugReportGUI(Player player) {
        int itemsPerPage = 27;
        int navigationRow = 36;

        UUID playerId = player.getUniqueId();
        List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

        totalPages = (int) Math.ceil((double) reports.size() / itemsPerPage);

        currentPage = Math.max(1, Math.min(currentPage, totalPages)); // Ensure the currentPage is within the valid range

        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.RESET + "Bug Reports - Page " + currentPage);

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, reports.size());
        for (int i = startIndex; i < endIndex; i++) {
            String report = reports.get(i);

            ItemStack reportItem = new ItemStack(Material.PAPER);
            ItemMeta meta = reportItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Bug Report #" + (i + 1));
            meta.setLore(Collections.singletonList(ChatColor.GRAY + report));
            reportItem.setItemMeta(meta);

            gui.setItem(i - startIndex, reportItem);
        }

        ItemStack backButton = createButton(Material.ARROW, ChatColor.GREEN + "Back");
        ItemStack forwardButton = createButton(Material.ARROW, ChatColor.GREEN + "Forward");
        ItemStack pageIndicator = createButton(Material.PAPER, ChatColor.YELLOW + "Page " + currentPage + " of " + totalPages);

        if (currentPage > 1) {
            gui.setItem(navigationRow, backButton);
        }
        if (currentPage < totalPages) {
            gui.setItem(navigationRow + 8, forwardButton);
        }

        gui.setItem(navigationRow + 4, pageIndicator);

        return gui;
    }

    private ItemStack createButton(Material material, String displayName) {
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
                int currentPage = reportManager.getCurrentPage();
                if (currentPage > 1) {
                    reportManager.setCurrentPage(currentPage - 1, player);
                }
            } else if (displayName.equals(ChatColor.GREEN + "Forward")) {
                int currentPage = reportManager.getCurrentPage();
                if (currentPage < reportManager.getTotalPages()) {
                    reportManager.setCurrentPage(currentPage + 1, player);
                }
            } else if (displayName.startsWith(ChatColor.YELLOW + "Bug Report #")) {
                int itemSlot = event.getSlot();
                int startIndex = (reportManager.getCurrentPage() - 1) * 27;
                int reportIndex = startIndex + itemSlot;
                UUID playerId = player.getUniqueId();
                List<String> reports = BugReportManager.bugReports.getOrDefault(playerId, new ArrayList<>());

                if (reportIndex >= 0 && reportIndex < reports.size()) {
                    String report = reports.get(reportIndex);
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

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setCurrentPage(int page, Player player) {
        if (currentPage == page) {
            return;
        }

        currentPage = Math.max(1, Math.min(page, totalPages));

        player.openInventory(getBugReportGUI(player));

        Inventory gui = player.getOpenInventory().getTopInventory();
        int navigationRow = 36;
        int pageIndicatorSlot = navigationRow + 4;
        ItemStack pageIndicator = createButton(Material.PAPER, ChatColor.YELLOW + "Page " + currentPage + " of " + totalPages);
        gui.setItem(pageIndicatorSlot, pageIndicator);

        ItemStack backButton = gui.getItem(navigationRow);
        if (currentPage == 1) {
            gui.setItem(navigationRow, null);
        } else if (backButton == null) {
            backButton = createButton(Material.ARROW, ChatColor.GREEN + "Back");
            gui.setItem(navigationRow, backButton);
        }

        ItemStack forwardButton = gui.getItem(navigationRow + 8);
        if (currentPage == totalPages) {
            gui.setItem(navigationRow + 8, null);
        } else if (forwardButton == null) {
            forwardButton = createButton(Material.ARROW, ChatColor.GREEN + "Forward");
            gui.setItem(navigationRow + 8, forwardButton);
        }
    }

    private static void openBugReportDetailsGUI(Player player, String report, Integer ID) {
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.YELLOW + "Bug Report Details - #" + ID);

        String[] reportLines = report.split("\n");

        String username = "";
        String uuid = "";
        String world = "";
        String fullMessage = "";

        boolean longMessage = false;

        for (String line : reportLines) {
            String trimmed = line.substring(line.indexOf(":") + 1).trim();

            if (line.startsWith("Username:")) username = trimmed;
            else if (line.startsWith("UUID:")) uuid = trimmed;
            else if (line.startsWith("World:")) world = trimmed;
            else if (line.startsWith("Full Message:")) fullMessage = trimmed;

            longMessage = fullMessage.length() > 32;
        }

        ItemStack emptyItem = createEmptyItem();
        ItemStack usernameItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "Username", ChatColor.WHITE + username);
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
            String currentLine = "";
            for (String word : words) {
                if (currentLine.length() + word.length() > 30) {
                    lore.add(currentLine);
                    currentLine = "";
                }

                currentLine += word + " ";
            }

            if (currentLine.length() > 0) {
                lore.add(currentLine);
            }

            meta.setLore(lore);
        } else {
            meta.setLore(Collections.singletonList(value));
        }

        item.setItemMeta(meta);

        return item;
    }
}
