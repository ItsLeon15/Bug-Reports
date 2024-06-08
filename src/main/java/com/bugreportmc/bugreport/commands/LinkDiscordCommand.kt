package com.bugreportmc.bugreport.commands

import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportManager.Companion.returnStartingMessage
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.annotations.Contract

class LinkDiscordCommand(private val reportManager: BugReportManager) : CommandExecutor {
	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage("Only players can use this command.")
			return true
		}

		if (sender.hasPermission("bugreport.admin")) {
			if (args.isEmpty()) {
				sender.sendMessage(returnStartingMessage(ChatColor.RED) + "Usage: /buglinkdiscord <webhook URL>")
				return true
			}

			val webhookURL = args[0]

			if (!isWebhookURLValid(webhookURL)) {
				sender.sendMessage(returnStartingMessage(ChatColor.RED) + "Invalid webhook URL.")
				return true
			}

			reportManager.setWebhookURL(webhookURL)

			sender.sendMessage(returnStartingMessage(ChatColor.GREEN) + "Webhook URL has been set successfully.")
		} else {
			sender.sendMessage(returnStartingMessage(ChatColor.RED) + "You don't have permission to use this command.")
		}

		return true
	}

	@Contract(pure = true)
	private fun isWebhookURLValid(webhookURL: String): Boolean {
		return webhookURL.matches("^https://(canary\\.)?discord\\.com/api/webhooks/[0-9]+/[a-zA-Z0-9-_]+$".toRegex())
	}
}
