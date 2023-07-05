package com.leon.bugreport;

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

import java.util.*;

import static com.leon.bugreport.BugReportManager.config;

public class BugReportCommand implements CommandExecutor, Listener {
    private final BugReportManager reportManager;
    private final Map<UUID, Integer> categorySelectionMap;
    private ItemStack categoryItem;

    public BugReportCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
        this.categorySelectionMap = new HashMap<>();
     }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (config.getBoolean("enablePluginReportCategories", true)) {
            if (!BugReportManager.checkCategoryConfig()) {
                player.sendMessage(ChatColor.RED + "Bug report categories are not configured correctly. Please contact an administrator.");
                return true;
            }
            openCategorySelectionGUI(player);
            return true;
        }

        if (args.length >= 1) {
            String message = String.join(" ", args);
            if (config.getInt("max-reports-per-player") != 0) {
                int maxReports = config.getInt("max-reports-per-player");
                int reportsLeft = maxReports - getReportCount(player.getUniqueId());

                if (reportsLeft <= 0) {
                    player.sendMessage(ChatColor.RED + "You have reached the maximum amount of reports you can submit.");
                    return true;
                }
            }
            reportManager.submitBugReport(player, message, null);

            if (config.getString("report-confirmation-message") == null) {
                player.sendMessage(ChatColor.GREEN + "Bug report submitted successfully!");
            } else {
                player.sendMessage(ChatColor.GREEN + config.getString("report-confirmation-message"));
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /bugreport <message>");
        }

        return true;
    }

    private int getReportCount(UUID playerId) {
        int count = 0;
        BugReportManager.bugReports.getOrDefault(playerId, new ArrayList<>());
        for (String report : BugReportManager.bugReports.get(playerId)) {
            if (report.contains(playerId.toString())) {
                count++;
            }
        } return count;
    }

    private void openCategorySelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Bug Report Categories");

        List<Category> categories = reportManager.getReportCategories();

        for (Category category : categories) {
            categoryItem = createCategoryItem(category);
            gui.addItem(categoryItem);
        }

        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.YELLOW + "Bug Report Categories")) {
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

        String categoryName = ChatColor.stripColor(itemMeta.getDisplayName());
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
            player.sendMessage(ChatColor.YELLOW + "Please enter your bug report in chat. Type 'cancel' to cancel.");
        }
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Integer categoryId = categorySelectionMap.get(player.getUniqueId());
        if (categoryId != null) {
            event.setCancelled(true);
            categorySelectionMap.remove(player.getUniqueId());
            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Bug report cancelled.");
                return;
            }
            reportManager.submitBugReport(player, message, categoryId);
            if (config.getString("report-confirmation-message") == null) {
                player.sendMessage(ChatColor.GREEN + "Bug report submitted successfully!");
            } else {
                player.sendMessage(ChatColor.GREEN + config.getString("report-confirmation-message"));
            }
        }
    }

    private ItemStack createCategoryItem(Category category) {
        ItemStack itemStack = new ItemStack(category.getItem());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(stringColorToColorCode(category.getColor()) + category.getName());
        itemMeta.setLore(List.of(ChatColor.GRAY + category.getDescription()));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ChatColor stringColorToColorCode(String color) {
        return switch (color) {
            case "AQUA" -> ChatColor.AQUA;
            case "BLACK" -> ChatColor.BLACK;
            case "BLUE" -> ChatColor.BLUE;
            case "DARK_AQUA" -> ChatColor.DARK_AQUA;
            case "DARK_BLUE" -> ChatColor.DARK_BLUE;
            case "DARK_GRAY" -> ChatColor.DARK_GRAY;
            case "DARK_GREEN" -> ChatColor.DARK_GREEN;
            case "DARK_PURPLE" -> ChatColor.DARK_PURPLE;
            case "DARK_RED" -> ChatColor.DARK_RED;
            case "GOLD" -> ChatColor.GOLD;
            case "GRAY" -> ChatColor.GRAY;
            case "GREEN" -> ChatColor.GREEN;
            case "LIGHT_PURPLE" -> ChatColor.LIGHT_PURPLE;
            case "RED" -> ChatColor.RED;
            case "WHITE" -> ChatColor.WHITE;
            case "YELLOW" -> ChatColor.YELLOW;
            default -> ChatColor.WHITE;
        };
    }
}

class Category {
    private final int id;
    private final String color;
    private final String name;
    private final ItemStack itemStack;

    public Category(int id, String name, String color, ItemStack itemStack) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.itemStack = itemStack;
    }

    public int getId() {
        return id;
    }

    public String getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasLore()) {
            return "";
        }
        return itemMeta.getLore().get(0);
    }

    public Material getItem() {
        return itemStack.getType();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
