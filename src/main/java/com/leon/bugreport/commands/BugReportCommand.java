package com.leon.bugreport.commands;

import com.leon.bugreport.BugReportManager;
import com.leon.bugreport.Category;
import com.leon.bugreport.DefaultLanguageSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportManager.*;
import static org.bukkit.ChatColor.*;

public class BugReportCommand implements CommandExecutor, Listener {
    private final BugReportManager reportManager;
    private final Map<UUID, Integer> categorySelectionMap;

    public BugReportCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
        this.categorySelectionMap = new HashMap<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (config.getBoolean("enablePluginReportCategories", true)) {
            if (!BugReportManager.checkCategoryConfig()) {
                player.sendMessage(pluginColor + pluginTitle + " " + RED + DefaultLanguageSelector.getTextElseDefault(language, "bugReportCategoriesNotConfiguredMessage"));
                return true;
            }
            openCategorySelectionGUI(player);
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(RED + "Usage: /bugreport <message>");
            return true;
        }

        int maxReports = config.getInt("max-reports-per-player");
        if (maxReports != 0) {
            int reportsLeft = maxReports - getReportCount(player.getUniqueId());
            if (reportsLeft <= 0) {
                if (checkForKey("useTitleInsteadOfMessage", true)) {
                    player.sendTitle(RED + DefaultLanguageSelector.getTextElseDefault(language, "maxReportsPerPlayerMessage"), "", 10, 70, 25);
                } else {
                    player.sendMessage (pluginColor + pluginTitle + " " + RED + DefaultLanguageSelector.getTextElseDefault(language, "maxReportsPerPlayerMessage"));
                }
                return true;
            }
        }

        try {
            reportManager.submitBugReport(player, String.join(" ", args), null);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to submit bug report");
            throw new RuntimeException(e);
        }
        if (checkForKey("useTitleInsteadOfMessage", true)) {
            player.sendTitle(GREEN + DefaultLanguageSelector.getTextElseDefault(language, "bugReportConfirmationMessage"), "", 10, 70, 25);
        } else {
            player.sendMessage(pluginColor + pluginTitle + " " + GREEN + DefaultLanguageSelector.getTextElseDefault(language, "bugReportConfirmationMessage"));
        }
        return true;
    }

    private int getReportCount(UUID playerId) {
        int count = 0;
        List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
        for (String report : reports) {
            if (report.contains(playerId.toString())) {
                count++;
            }
        }
        return count;
    }

    private void openCategorySelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, YELLOW + "Bug Report Categories");

        List<Category> categories = reportManager.getReportCategories();

        for (Category category : categories) {
            ItemStack categoryItem = createCategoryItem(category);
            gui.addItem(categoryItem);
        }

        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(YELLOW + "Bug Report Categories")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta itemMeta = clickedItem.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        String categoryName = stripColor(itemMeta.getDisplayName());
        List<Category> categories = reportManager.getReportCategories();
        Category selectedCategory = null;

        for (Category category : categories) {
            if (category.getName().equals(categoryName)) {
                selectedCategory = category;
                break;
            }
        }

        if (selectedCategory != null) {
            categorySelectionMap.put(player.getUniqueId(), selectedCategory.getId());
            player.closeInventory();
            if (checkForKey("useTitleInsteadOfMessage", true)) {
                player.sendTitle(YELLOW + DefaultLanguageSelector.getTextElseDefault(language, "enterBugReportMessageCategory"), "", 10, 70, 120); // TODO: Finish this in all places
            } else {
                player.sendMessage(pluginColor + pluginTitle + " " + YELLOW + DefaultLanguageSelector.getTextElseDefault(language, "enterBugReportMessageCategory"));
            }
        } else {
            player.sendMessage(pluginColor + pluginTitle + " " + RED + "Something went wrong while selecting the category");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(@NotNull AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Integer categoryId = categorySelectionMap.get(player.getUniqueId());

        if (categoryId == null) {
            return;
        }

        event.setCancelled(true);
        categorySelectionMap.remove(player.getUniqueId());
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            if (checkForKey("useTitleInsteadOfMessage", true)) {
                player.sendTitle(RED + DefaultLanguageSelector.getTextElseDefault(language, "cancelledBugReportMessage"), "", 10, 70, 25);
            } else {
                player.sendMessage(pluginColor + pluginTitle + " " + RED + DefaultLanguageSelector.getTextElseDefault(language, "cancelledBugReportMessage"));
            }
            return;
        }

        reportManager.submitBugReport(player, message, categoryId);
        if (checkForKey("useTitleInsteadOfMessage", true)) {
            player.sendTitle(GREEN + DefaultLanguageSelector.getTextElseDefault(language, "bugReportConfirmationMessage"), "", 10, 70, 25);
        } else {
            player.sendMessage(pluginColor + pluginTitle + " " + GREEN + DefaultLanguageSelector.getTextElseDefault(language, "bugReportConfirmationMessage"));
        }
    }

    private @NotNull ItemStack createCategoryItem(@NotNull Category category) {
        ItemStack itemStack = new ItemStack(category.getItem());
        ItemMeta itemMeta = itemStack.getItemMeta();
        Objects.requireNonNull(itemMeta).setDisplayName(stringColorToColorCode(category.getColor()) + category.getName());
        itemMeta.setLore(List.of(GRAY + category.getDescription()));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Contract(pure = true)
    public static ChatColor stringColorToColorCode(String color) {
        if (color == null) {
            return WHITE;
        }
        return switch (color.toUpperCase()) {
            case "AQUA" -> AQUA;
            case "BLACK" -> BLACK;
            case "BLUE" -> BLUE;
            case "DARK_AQUA" -> DARK_AQUA;
            case "DARK_BLUE" -> DARK_BLUE;
            case "DARK_GRAY" -> DARK_GRAY;
            case "DARK_GREEN" -> DARK_GREEN;
            case "DARK_PURPLE" -> DARK_PURPLE;
            case "DARK_RED" -> DARK_RED;
            case "GOLD" -> GOLD;
            case "GRAY" -> GRAY;
            case "GREEN" -> GREEN;
            case "LIGHT_PURPLE" -> LIGHT_PURPLE;
            case "RED" -> RED;
            case "WHITE" -> WHITE;
            case "YELLOW" -> YELLOW;
            default -> WHITE;
        };
    }

    @Contract(pure = true)
    public static java.awt.Color chatColorToColor(ChatColor color) {
        if (color == null) {
            return java.awt.Color.WHITE;
        }
        return switch (color) {
			case AQUA -> java.awt.Color.CYAN;
            case BLACK -> java.awt.Color.BLACK;
            case BLUE -> java.awt.Color.BLUE;
            case DARK_AQUA -> java.awt.Color.CYAN.darker();
            case DARK_BLUE -> java.awt.Color.BLUE.darker();
            case DARK_GRAY -> java.awt.Color.GRAY.darker();
            case DARK_GREEN -> java.awt.Color.GREEN.darker();
            case DARK_PURPLE -> java.awt.Color.MAGENTA.darker();
            case DARK_RED -> java.awt.Color.RED.darker();
            case GOLD -> java.awt.Color.ORANGE;
            case GRAY -> java.awt.Color.GRAY;
            case GREEN -> java.awt.Color.GREEN;
            case LIGHT_PURPLE -> java.awt.Color.MAGENTA;
            case RED -> java.awt.Color.RED;
            case WHITE -> java.awt.Color.WHITE;
            case YELLOW -> java.awt.Color.YELLOW;
            default -> java.awt.Color.WHITE;
        };
    }
}