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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BugReportCommand implements CommandExecutor, Listener {
    private final BugReportManager reportManager;
    private final Map<UUID, Integer> categorySelectionMap;

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

        if (BugReportManager.config.getBoolean("enablePluginReportCategories", true)) {
            openCategorySelectionGUI(player);
            return true;
        }

        if (args.length >= 1) {
            String message = String.join(" ", args);
            reportManager.submitBugReport(player, message, null);
            player.sendMessage(ChatColor.GREEN + "Bug report submitted successfully!");
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /bugreport <message>");
        }

        return true;
    }

    private void openCategorySelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.RESET + "Bug Report Categories");

        List<Category> categories = reportManager.getReportCategories();

        for (Category category : categories) {
            ItemStack categoryItem = createCategoryItem(category);
            gui.addItem(categoryItem);
        }

        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.RESET + "Bug Report Categories")) {
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
            player.sendMessage(ChatColor.YELLOW + "Please enter your bug report message in chat.");
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
            reportManager.submitBugReport(player, message, categoryId);
            player.sendMessage(ChatColor.GREEN + "Bug report submitted successfully!");
        }
    }

    private ItemStack createCategoryItem(Category category) {
        ItemStack itemStack = new ItemStack(category.getItem());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + category.getName());
        itemMeta.setLore(List.of(ChatColor.GRAY + category.getDescription()));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}

class Category {
    private final int id;
    private final String name;
    private final ItemStack itemStack;

    public Category(int id, String name, ItemStack itemStack) {
        this.id = id;
        this.name = name;
        this.itemStack = itemStack;
    }

    public int getId() {
        return id;
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
