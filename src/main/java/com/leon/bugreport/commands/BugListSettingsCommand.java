package com.leon.bugreport.commands;

import com.leon.bugreport.API.ErrorClass;
import com.leon.bugreport.BugReportManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.leon.bugreport.BugReportManager.debugMode;
import static com.leon.bugreport.BugReportManager.returnStartingMessage;
import static com.leon.bugreport.BugReportSettings.getSettingsGUI;

public class BugListSettingsCommand implements CommandExecutor {

	public BugListSettingsCommand(BugReportManager reportManager) {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (debugMode) {
			ErrorClass.throwDebug("BugListSettingsCommand: Triggered /buglistsettings command");
		}
		if (!(sender instanceof Player player)) {
			sender.sendMessage(returnStartingMessage(ChatColor.RED) + "This command can only be run by a player.");
			return true;
		}
		if (player.hasPermission("bugreport.admin") || player.hasPermission("bugreport.settings")) {
			player.openInventory(getSettingsGUI());
		} else {
			player.sendMessage(returnStartingMessage(ChatColor.RED)
					+ "You don't have permission to use this command.");
		}

		return true;
	}
}
