package com.leon.bugreport.gui;

import com.leon.bugreport.BugReportDatabase;
import com.leon.bugreport.BugReportLanguage;
import com.leon.bugreport.BugReportManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.BugReportSettings.createCustomPlayerHead;

public class BugReportConfirmationGUI {
	public static void openConfirmationGUI(@NotNull Player player, @NotNull Boolean isArchived) {
		player.openInventory(getConfirmationGUI(isArchived));
	}

	public static @NotNull Inventory getConfirmationGUI(boolean isArchived) {
		String guiTitle;

		if (isArchived) {
			guiTitle = ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("confirmationArchive");
		} else {
			guiTitle = ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("confirmationDelete");
		}

		Inventory gui = Bukkit.createInventory(null, 27, guiTitle);

		ItemStack backButton = createButton(Material.BARRIER, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("back"));
		gui.setItem(15, backButton);

		if (isArchived) {
			ItemStack archiveButton = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y5YjY3YmI5Y2MxYzg4NDg2NzYwYjE3MjY1MDU0MzEyZDY1OWRmMmNjNjc1NTc1MDA0NWJkNzFjZmZiNGU2MCJ9fX0=", ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("archive"), 16);
//			ItemStack archiveButton = createButton(Material.EMERALD, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("archive"));
			gui.setItem(11, archiveButton);
		} else {
			ItemStack deleteButton = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmUwZmQxMDE5OWU4ZTRmY2RhYmNhZTRmODVjODU5MTgxMjdhN2M1NTUzYWQyMzVmMDFjNTZkMThiYjk0NzBkMyJ9fX0=", ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("delete"), 18);
//			ItemStack deleteButton = createButton(Material.REDSTONE, ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("delete"));
			gui.setItem(11, deleteButton);
		}

		return gui;
	}

	public void archiveReport(@NotNull Player player, @NotNull Integer reportIDGUI, @NotNull Boolean isArchivedDetails) {
		BugReportDatabase.updateBugReportArchive(reportIDGUI, 1);
		player.openInventory(isArchivedDetails ? getArchivedBugReportsGUI(player) : getBugReportGUI(player));
		player.sendMessage(pluginColor + pluginTitle + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + " Bug Report #" + reportIDGUI + " has been archived.");
	}

	public void deleteReport(@NotNull Player player, @NotNull Integer reportIDGUI, @NotNull Boolean isArchivedDetails) {
		UUID playerId = player.getUniqueId();
		BugReportDatabase.deleteBugReport(reportIDGUI);

		List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
		reports.removeIf(report -> report.contains("Report ID: " + reportIDGUI));
		bugReports.put(playerId, reports);

		player.openInventory(isArchivedDetails ? getArchivedBugReportsGUI(player) : getBugReportGUI(player));
		player.sendMessage(pluginColor + pluginTitle + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.RED) + " Bug Report #" + reportIDGUI + " has been deleted.");
	}

	public record BugReportConfirmationListener(Inventory gui, Integer reportIDGUI,
	                                            Boolean fromArchivedGUI) implements Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String TitleText = ChatColor.stripColor(event.getView().getTitle());

			if (BugReportManager.debugMode) plugin.getLogger().info("Clicked inventory: " + TitleText);

			boolean isArchivedDetails = TitleText.startsWith("Archive Bug Report?");
			boolean isDeletedDetails = TitleText.startsWith("Delete Bug Report?");

			if (!isArchivedDetails && !isDeletedDetails) {
				return;
			}

			event.setCancelled(true);

			Player player = (Player) event.getWhoClicked();
			Inventory clickedInventory = event.getClickedInventory();
			if (clickedInventory == null) {
				return;
			}

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) {
				return;
			}

			ItemMeta itemMeta = clickedItem.getItemMeta();
			if (itemMeta == null || !itemMeta.hasDisplayName()) {
				return;
			}

			String displayName = itemMeta.getDisplayName();
			String customDisplayName = BugReportLanguage.getEnglishVersionFromLanguage(ChatColor.stripColor(displayName));

			if (BugReportManager.debugMode) plugin.getLogger().info("Clicked item: " + customDisplayName);

			if (isArchivedDetails) {
				if (BugReportManager.debugMode) plugin.getLogger().info("Opening archived confirmation GUI.");
				switch (customDisplayName) {
					case "Archive" -> {
						player.playSound(player.getLocation(), "ui.button.click", 0.6F, 1.0F);

						if (BugReportManager.debugMode) plugin.getLogger().info("Archiving report: " + reportIDGUI);
						new BugReportConfirmationGUI().archiveReport(player, reportIDGUI, true);

						player.closeInventory();

						HandlerList.unregisterAll(this);
					}
					case "Back" -> {
						player.playSound(player.getLocation(), "ui.button.click", 0.6F, 1.0F);

						if (BugReportManager.debugMode) plugin.getLogger().info("Going back to bug reports.");
						player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(player) : getBugReportGUI(player));

						HandlerList.unregisterAll(this);
					}
				}
			}

			if (isDeletedDetails) {
				if (BugReportManager.debugMode) plugin.getLogger().info("Opening delete confirmation GUI.");
				switch (customDisplayName) {
					case "Delete" -> {
						player.playSound(player.getLocation(), "ui.button.click", 0.6F, 1.0F);

						if (BugReportManager.debugMode) plugin.getLogger().info("Deleting report: " + reportIDGUI);
						new BugReportConfirmationGUI().deleteReport(player, reportIDGUI, isArchivedDetails);

						player.closeInventory();

						HandlerList.unregisterAll(this);
					}
					case "Back" -> {
						player.playSound(player.getLocation(), "ui.button.click", 0.6F, 1.0F);

						if (BugReportManager.debugMode) plugin.getLogger().info("Going back to archived reports.");
						player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(player) : getBugReportGUI(player));

						HandlerList.unregisterAll(this);
					}
				}
			}
		}
	}
}
