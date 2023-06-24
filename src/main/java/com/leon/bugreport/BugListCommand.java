package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class BugListCommand implements CommandExecutor {
    private BugReportManager reportManager;

    public BugListCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPermission("bugreport.admin")) {
            Inventory bugReportGUI = reportManager.getBugReportGUI(player);
            player.openInventory(bugReportGUI);
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
        }

        return true;
    }
}
