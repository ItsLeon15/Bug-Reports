package com.bugreportmc.bugreport.commands

import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportManager.Companion.getArchivedBugReportsGUI
import com.bugreportmc.bugreport.BugReportManager.Companion.returnStartingMessage
import com.bugreportmc.bugreport.BugReportManager.Companion.setCurrentPage
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class BugListArchivedCommand : CommandExecutor {
	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage(returnStartingMessage(ChatColor.RED) + "This command can only be run by a player.")
			return true
		}

		if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.archived")) {
			setCurrentPage(sender, 1)
			val bugReportGUI: Inventory = getArchivedBugReportsGUI(BugReportManager.localCurrentPage, sender)
			sender.openInventory(bugReportGUI)
		} else {
			sender.sendMessage(returnStartingMessage(ChatColor.RED) + "You don't have permission to use this command.")
		}

		return true
	}
}
