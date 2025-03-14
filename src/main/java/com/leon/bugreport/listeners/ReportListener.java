package com.leon.bugreport.listeners;

import com.leon.bugreport.BugReportManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

public class ReportListener implements Listener {
	@EventHandler
	public void onReportCreated(ReportCreatedEvent event) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getOpenInventory().getTitle().startsWith(ChatColor.YELLOW + "Bug Report")) {
				Inventory newGui = BugReportManager.generateBugReportGUI(1, false);
				player.openInventory(newGui);
			}
		}
	}
}
