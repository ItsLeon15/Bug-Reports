package com.leon.bugreport.commands;

import com.leon.bugreport.BugReportManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

import static com.leon.bugreport.BugReportManager.*;

public class BugListArchivedCommand implements CommandExecutor {

	public BugListArchivedCommand(BugReportManager reportManager) {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "This command can only be run by a player.");
			return true;
		}

		if (player.hasPermission("bugreport.admin") || player.hasPermission("bugreport.archived")) {
			BugReportManager.setCurrentPage(player, 1);
			Inventory bugReportGUI = BugReportManager.getArchivedBugReportsGUI(player);
			player.openInventory(bugReportGUI);
		} else {
			player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "You don't have permission to use this command.");
		}

		return true;
	}
}
