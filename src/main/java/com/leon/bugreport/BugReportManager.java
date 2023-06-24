package com.leon.bugreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class BugReportManager {

    private static Map<UUID, List<String>> bugReports;
    private BugReportDatabase database;
    private static Plugin plugin;

    public BugReportManager(Plugin plugin, String dbFilePath) {
        BugReportManager.plugin = plugin;
        bugReports = new HashMap<>();
        database = new BugReportDatabase(dbFilePath);
        loadBugReports();
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
    }

    public Inventory getBugReportGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, ChatColor.GREEN + "Bug Reports");

        UUID playerId = player.getUniqueId();
        List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

        for (int i = 0; i < Math.min(reports.size(), 9); i++) {
            String report = reports.get(i);

            ItemStack reportItem = new ItemStack(Material.PAPER);
            ItemMeta meta = reportItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Bug Report #" + (i + 1));
            meta.setLore(List.of(ChatColor.GRAY + report));
            reportItem.setItemMeta(meta);

            gui.setItem(i, reportItem);
        }

        return gui;
    }

    private void loadBugReports() {
        Map<UUID, List<String>> loadedReports = database.loadBugReports();
        if (loadedReports != null) {
            bugReports = loadedReports;
        }
    }

    public static class BugReportListener implements org.bukkit.event.Listener {
        private final BugReportManager reportManager;
        private Plugin plugin;

        public BugReportListener(BugReportManager reportManager, Plugin plugin) {
            this.reportManager = reportManager;
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onInventoryClick(InventoryClickEvent event) {
            if (!event.getView().getTitle().equals(ChatColor.GREEN + "Bug Reports"))
                return;

            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() != Material.PAPER)
                return;

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName())
                return;

            String displayName = meta.getDisplayName();
            if (!displayName.startsWith(ChatColor.YELLOW.toString() + "Bug Report #"))
                return;

            String reportIndexString = displayName.replace(ChatColor.YELLOW.toString() + "Bug Report #", "");
            try {
                int reportIndex = Integer.parseInt(reportIndexString) - 1;

                Player player = (Player) event.getWhoClicked();
                UUID playerId = player.getUniqueId();
                List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());

                if (reportIndex >= 0 && reportIndex < reports.size()) {
                    String report = reports.get(reportIndex);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openBugReportDetailsGUI(player, report), 1);
                } else {
                    plugin.getLogger().warning("Bug report index is out of bounds!");
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid bug report index!");
                e.printStackTrace();
            }
        }
    }

    private static void openBugReportDetailsGUI(Player player, String report) {
        System.out.println("Opening Bug Report Details GUI...");

        Inventory gui = Bukkit.createInventory(player, 9, ChatColor.YELLOW + "Bug Report Details");

        String[] reportLines = report.split("\n");

        String username = "";
        String uuid = "";
        String world = "";
        String fullMessage = "";

        plugin.getLogger().info("Report Lines: " + Arrays.toString(reportLines));

        boolean longMessage = false;

        for (String line : reportLines) {
            if (line.startsWith("Username:")) {
                username = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("UUID:")) {
                uuid = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("World:")) {
                world = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("Full Message:")) {
                fullMessage = line.substring(line.indexOf(":") + 1).trim();
                if (fullMessage.length() > 32) {
                    longMessage = true;
                } else {
                    longMessage = false;
                }
            }
        }

        ItemStack emptyItem = createEmptyItem();
        ItemStack usernameItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "Username", ChatColor.WHITE + username);
        ItemStack uuidItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "UUID", ChatColor.WHITE + uuid);
        ItemStack worldItem = createInfoItem(Material.GRASS_BLOCK, ChatColor.GOLD + "World", ChatColor.WHITE + world);
        ItemStack messageItem = createInfoItem(Material.PAPER, ChatColor.GOLD + "Full Message", ChatColor.WHITE + fullMessage, longMessage);

        gui.setItem(1, usernameItem);
        gui.setItem(3, uuidItem);
        gui.setItem(5, worldItem);
        gui.setItem(7, messageItem);

        for (int i = 0; i < 9; i += 2) {
            gui.setItem(i, emptyItem);
        }

        player.openInventory(gui);
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
                meta.setLore(List.of(value));
            }

            item.setItemMeta(meta);

            return item;
        }
    }