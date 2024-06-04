package com.leon.bugreport.commands;

import com.leon.bugreport.API.ErrorClass;
import com.leon.bugreport.BugReportManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.leon.bugreport.BugReportManager.debugMode;
import static com.leon.bugreport.BugReportManager.returnStartingMessage;

public class LinkDiscordCommand implements CommandExecutor {
	private final BugReportManager reportManager;

	public LinkDiscordCommand(BugReportManager reportManager) {
		this.reportManager = reportManager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (debugMode) {
			ErrorClass.throwDebug("LinkDiscordCommand: Triggered /buglistdiscord", "debug");
		}
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		if (player.hasPermission("bugreport.admin")) {
			if (debugMode) {
				ErrorClass.throwDebug("LinkDiscordCommand: Passed permission check", "debug");
			}
			if (args.length < 1) {
				player.sendMessage(returnStartingMessage(ChatColor.RED) + "Usage: /buglinkdiscord <webhook URL>");
				return true;
			}

			String webhookURL = args[0];

			if (!isWebhookURLValid(webhookURL)) {
				player.sendMessage(returnStartingMessage(ChatColor.RED) + "Invalid webhook URL.");
				return true;
			}

			reportManager.setWebhookURL(webhookURL);

			player.sendMessage(returnStartingMessage(ChatColor.GREEN) + "Webhook URL has been set successfully.");
		} else {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + "You don't have permission to use this command.");
		}

		return true;
	}

	@Contract(pure = true)
	private boolean isWebhookURLValid(@NotNull String webhookURL) {
		if (debugMode) {
			ErrorClass.throwDebug("LinkDiscordCommand: Starting isWebhookURLValid", "debug");
		}
		return webhookURL.matches("^https://(canary\\.)?discord\\.com/api/webhooks/[0-9]+/[a-zA-Z0-9-_]+$");
	}
}
