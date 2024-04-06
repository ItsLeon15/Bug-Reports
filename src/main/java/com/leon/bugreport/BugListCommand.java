package com.leon.bugreport;

import com.leon.bugreport.listeners.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.leon.bugreport.BugReportManager.*;

public class BugListCommand implements CommandExecutor {
	public BugListCommand(BugReportManager reportManager) {
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "This command can only be run by a player.");
			return true;
		}

		if (!player.hasPermission("bugreport.admin")) {
			if (args.length == 0) {
				player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "You don't have permission to use this command.");
				return true;
			}

			if (args[0].equalsIgnoreCase("help")) {
				player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GREEN) + "Commands:");
				player.sendMessage(ChatColor.GOLD + "/bugreport <Message>" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Submits a bug report.");
				player.sendMessage(ChatColor.GOLD + "/bugreport help" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Displays this help message.");
				return true;
			}

			player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "You don't have permission to use this command.");
			return true;
		}

		if (args.length == 0) {
			BugReportManager.setCurrentPage(player, 1);
			Inventory bugReportGUI = BugReportManager.getBugReportGUI(player);
			player.openInventory(bugReportGUI);
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "help" -> {
				String commandFormat = ChatColor.GOLD + "/%s" + ChatColor.WHITE + " - " + ChatColor.GRAY + "%s\n";
				String message = pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GREEN) + "Admin Commands:\n" + String.format(commandFormat, "bugreport", "Submits a bug report.") + String.format(commandFormat, "buglist", "Opens the buglist GUI.") + String.format(commandFormat, "buglist help", "Displays this help message.") + String.format(commandFormat, "buglist reload", "Reloads the plugin and config.") + String.format(commandFormat, "buglistarchived", "Opens the buglist archived GUI.") + String.format(commandFormat, "buglistsettings", "Opens the buglist settings GUI.") + String.format(commandFormat, "buglinkdiscord", "Links Bug Report to a Discord channel.");

				player.sendMessage(message);
			}
			case "reload" -> {
				BugReportLanguage.loadLanguageTexts(plugin, "languages.yml");
				BugReportManager.loadConfig();
				plugin.reloadConfig();
				BugReportDatabase.reloadConnection();

				player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GREEN) + "The plugin has been reloaded.");
			}
			case "debug" -> {
				if (args.length < 2) {
					player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "Please enter a number between 0 and 1.");
					return true;
				}

				try {
					int debugMode = Integer.parseInt(args[1]);
					if (debugMode >= 0 && debugMode <= 1) {
						BugReportManager.setDebugMode(debugMode);
						player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GREEN) + "Debug mode set to " + debugMode + ".");
					} else {
						player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "Please enter a number between 0 and 1.");
					}
				} catch (NumberFormatException e) {
					player.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + "Please enter a valid number.");
				}
			}
			case "version" -> {
				StringBuilder versionMessage = new StringBuilder(pluginColor + "----------===== " + ChatColor.GOLD + pluginTitle + pluginColor + " =====----------\n");

				new UpdateChecker((JavaPlugin) plugin, 110732).getVersion(version -> {
					String pluginVersion = plugin.getDescription().getVersion();
					String pluginURL = "https://www.spigotmc.org/resources/bug-report.110732/";
					String pluginDesc = "Bug Report is a plugin that let's players submit bug reports to server admins";
					String pluginAuthor = "ItsLeon15";

					versionMessage.append(ChatColor.GOLD).append("Version: ").append(pluginVersion.equalsIgnoreCase(version) ? ChatColor.GREEN : ChatColor.RED).append(pluginVersion).append(pluginVersion.equalsIgnoreCase(version) ? " (Up to date)" : " (Out of date)").append("\n");
					versionMessage.append(ChatColor.GOLD).append("URL: ").append(ChatColor.WHITE).append(pluginURL).append("\n");
					versionMessage.append(ChatColor.GOLD).append("Description: ").append(ChatColor.WHITE).append(pluginDesc).append("\n");
					versionMessage.append(ChatColor.GOLD).append("Author: ").append(ChatColor.WHITE).append(pluginAuthor).append("\n");
					player.sendMessage(versionMessage.toString());
				});
			}
			default -> {
				BugReportManager.setCurrentPage(player, 1);
				Inventory bugReportGUI = BugReportManager.getBugReportGUI(player);
				player.openInventory(bugReportGUI);
			}
		}

		return true;
	}
}
