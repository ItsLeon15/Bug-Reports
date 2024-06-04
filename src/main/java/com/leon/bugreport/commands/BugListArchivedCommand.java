package com.leon.bugreport.commands;

import com.leon.bugreport.API.ErrorClass;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import static com.leon.bugreport.BugReportManager.*;

public class BugListArchivedCommand implements CommandExecutor {

	public BugListArchivedCommand() {
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (debugMode) {
			ErrorClass.throwDebug("BugListArchivedCommand: Triggered /buglistarchived command", "debug");
		}
		if (!(sender instanceof Player player)) {
			sender.sendMessage(returnStartingMessage(ChatColor.RED) + "This command can only be run by a player.");
			return true;
		}

		if (player.hasPermission("bugreport.admin") || player.hasPermission("bugreport.archived")) {
			setCurrentPage(player, 1);
			Inventory bugReportGUI = getArchivedBugReportsGUI(localCurrentPage, player);
			player.openInventory(bugReportGUI);
		} else {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + "You don't have permission to use this command.");
		}

		return true;
	}
}
