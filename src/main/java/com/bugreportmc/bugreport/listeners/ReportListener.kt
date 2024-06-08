package com.bugreportmc.bugreport.listeners

import com.bugreportmc.bugreport.BugReportManager.Companion.generateBugReportGUI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.Inventory

class ReportListener : Listener {
	@EventHandler
	fun onReportCreated(event: ReportCreatedEvent?) {
		for (player in Bukkit.getOnlinePlayers()) {
			if (player.openInventory.title.startsWith(ChatColor.YELLOW.toString() + "Bug Report")) {
				val newGui: Inventory = generateBugReportGUI(1, player, false)
				player.openInventory(newGui)
			}
		}
	}
}
