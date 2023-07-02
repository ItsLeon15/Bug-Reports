package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BugListCommand implements CommandExecutor {
    private final BugReportManager reportManager;

    public BugListCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (player.hasPermission("bugreport.admin")) {
            BugReportManager.setCurrentPage(player, 1);

            Inventory bugReportGUI = BugReportManager.getBugReportGUI(player);

            ItemStack backButton = null;
            if (BugReportManager.getCurrentPage(player) == 1) {
                bugReportGUI.setItem(36, new ItemStack(Material.AIR));
            } else {
                backButton = new ItemStack(Material.ARROW);
                ItemMeta backMeta = backButton.getItemMeta();
                backMeta.setDisplayName(ChatColor.GREEN + "Back");
                backButton.setItemMeta(backMeta);
            }

            ItemStack forwardButton = new ItemStack(Material.ARROW);
            ItemMeta forwardMeta = forwardButton.getItemMeta();
            forwardMeta.setDisplayName(ChatColor.GREEN + "Forward");
            forwardButton.setItemMeta(forwardMeta);

            bugReportGUI.setItem(36, backButton);
            bugReportGUI.setItem(44, forwardButton);

            player.openInventory(bugReportGUI);
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
        }

        return true;
    }
}
