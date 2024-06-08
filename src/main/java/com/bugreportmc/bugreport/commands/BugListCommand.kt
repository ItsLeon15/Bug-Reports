package com.bugreportmc.bugreport.commands

import com.bugreportmc.bugreport.BugReportDatabase.Companion.reloadConnection
import com.bugreportmc.bugreport.BugReportLanguage.Companion.loadLanguageFiles
import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportManager.Companion.reloadConfig
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.listeners.UpdateChecker
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class BugListCommand : CommandExecutor {
	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage(
				BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.RED
				) + "This command can only be run by a player."
			)
			return true
		}
		if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.list") || sender.isOp()) {
			if (args.isEmpty()) {
				BugReportManager.setCurrentPage(sender, 1)
				val bugReportGUI = BugReportManager.getBugReportGUI(1, sender)
				sender.openInventory(bugReportGUI)
				return true
			}

			when (args[0].lowercase(Locale.getDefault())) {
				"help" -> returnHelpCommand(sender)
				"reload" -> returnReloadCommand(sender)
				"debug" -> returnDebugCommand(sender, args)
				"version" -> returnVersionCommand(sender)
				else -> returnDefaultCommand(sender)
			}
			return true
		}

		if (args.isEmpty()) {
			sender.sendMessage(
				BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.RED
				) + "You don't have permission to use this command."
			)
			return true
		}

		if (args[0].equals("help", ignoreCase = true)) {
			sender.sendMessage(
				BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.GREEN
				) + "Commands:"
			)
			sender.sendMessage(ChatColor.GOLD.toString() + "/bugreport <Message>" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Submits a bug report.")
			sender.sendMessage(ChatColor.GOLD.toString() + "/bugreport help" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Displays this help message.")
			return true
		}

		sender.sendMessage(
			BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
				BugReportManager.endingPluginTitleColor, ChatColor.RED
			) + "You don't have permission to use this command."
		)
		return true
	}

	private fun returnReloadCommand(player: Player) {
		reloadConfig()
		loadLanguageFiles()
		plugin.reloadConfig()
		reloadConnection()

		player.sendMessage(
			BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
				BugReportManager.endingPluginTitleColor, ChatColor.GREEN
			) + "The plugin has been reloaded."
		)
	}

	private fun returnDebugCommand(player: Player, args: Array<String>) {
		if (args.size < 2) {
			player.sendMessage(
				BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.RED
				) + "Please enter a number between 0 and 1."
			)
			return
		}

		try {
			val debugMode = args[1].toInt()
			if (debugMode >= 0 && debugMode <= 1) {
				BugReportManager.setDebugMode(debugMode)
				player.sendMessage(
					BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
						BugReportManager.endingPluginTitleColor, ChatColor.GREEN
					) + "Debug mode set to " + debugMode + "."
				)
			} else {
				player.sendMessage(
					BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
						BugReportManager.endingPluginTitleColor, ChatColor.RED
					) + "Please enter a number between 0 and 1."
				)
			}
		} catch (e: NumberFormatException) {
			player.sendMessage(
				BugReportManager.pluginColor.toString() + BugReportManager.pluginTitle + " " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.RED
				) + "Please enter a valid number."
			)
		}
	}

	private fun returnVersionCommand(player: Player) {
		val versionMessage =
			StringBuilder(BugReportManager.pluginColor.toString() + "----------===== " + ChatColor.GOLD + BugReportManager.pluginTitle + BugReportManager.pluginColor + " =====----------\n")

		UpdateChecker(plugin, 110732).getVersion { version: String? ->
			val pluginVersion = plugin.description.version
			val pluginURL = "https://www.spigotmc.org/resources/bug-report-1-16-4-1-20-5.110732/"
			val pluginDescription = "Bug Report is a plugin that lets players submit bug reports to server admins"
			val pluginAuthor = "ItsLeon15"

			versionMessage.append(ChatColor.GOLD).append("Version: ")
				.append(if (pluginVersion.equals(version, ignoreCase = true)) ChatColor.GREEN else ChatColor.RED)
				.append(pluginVersion.toString())
				.append(if (pluginVersion.equals(version, ignoreCase = true)) " (Up to date)" else " (Out of date)")
				.append("\n")
			versionMessage.append(ChatColor.GOLD).append("URL: ").append(ChatColor.WHITE).append(pluginURL).append("\n")
			versionMessage.append(ChatColor.GOLD).append("Description: ").append(ChatColor.WHITE)
				.append(pluginDescription).append("\n")
			versionMessage.append(ChatColor.GOLD).append("Author: ").append(ChatColor.WHITE).append(pluginAuthor)
				.append("\n")
			player.sendMessage(versionMessage.toString())
		}
	}

	private fun returnDefaultCommand(player: Player) {
		BugReportManager.setCurrentPage(player, 1)
		val bugReportGUI = BugReportManager.getBugReportGUI(1, player)
		player.openInventory(bugReportGUI)
	}

	companion object {
		fun returnHelpCommand(player: Player) {
			val commandFormat = ChatColor.GOLD.toString() + "/%s" + ChatColor.WHITE + " - " + ChatColor.GRAY + "%s\n"

			val messageBuilder = StringBuilder()
			messageBuilder.append(BugReportManager.pluginColor).append(BugReportManager.pluginTitle).append(" ")
				.append(Objects.requireNonNullElse(BugReportManager.endingPluginTitleColor, ChatColor.GREEN))
				.append("Admin Commands:\n")

			val commands = arrayOf(
				arrayOf("bugreport", "Submits a bug report."),
				arrayOf("buglist", "Opens the buglist GUI."),
				arrayOf("buglist help", "Displays this help message."),
				arrayOf("buglist reload", "Reloads the plugin and config."),
				arrayOf("buglist debug <0/1>", "Sets the debug mode."),
				arrayOf("buglist version", "Displays the plugin version."),
				arrayOf("buglistarchived", "Opens the buglist archived GUI."),
				arrayOf("buglistsettings", "Opens the buglist settings GUI."),
				arrayOf("buglinkdiscord", "Links Bug Report to a Discord channel.")
			)

			for (helpCommand in commands) {
				messageBuilder.append(String.format(commandFormat, helpCommand[0], helpCommand[1]))
			}

			player.sendMessage(messageBuilder.toString())
		}
	}
}
