package com.leon.bugreport.gui;

import com.leon.bugreport.BugReportDatabase;
import com.leon.bugreport.BugReportLanguage;
import com.leon.bugreport.keys.guiTextures;
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

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportLanguage.getKeyFromTranslation;
import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.BugReportSettings.createCustomPlayerHead;
import static com.leon.bugreport.gui.bugreportGUI.openBugReportDetailsGUI;

public class BugReportConfirmationGUI {
	public static void openConfirmationGUI(@NotNull Player player, @NotNull Boolean isArchived, String bugReportID) {
		player.openInventory(getConfirmationGUI(isArchived, bugReportID));
	}

	public static @NotNull Inventory getConfirmationGUI(boolean isArchived, String bugReportID) {
		// TODO: Add the bug report ID to the title without any destructive actions.
		String guiTitle;

		if (isArchived) {
			guiTitle = ChatColor.YELLOW + Objects.requireNonNull(BugReportLanguage.getValueFromLanguageFile("buttonNames.confirmationArchive", "Archive Bug Report?"));
		} else {
			guiTitle = ChatColor.YELLOW + Objects.requireNonNull(BugReportLanguage.getValueFromLanguageFile("buttonNames.confirmationDelete", "Delete Bug Report?"));
		}

		Inventory gui = Bukkit.createInventory(null, 27, guiTitle);
		ItemStack backButton = createButton(Material.BARRIER, ChatColor.YELLOW + BugReportLanguage.getValueFromLanguageFile("buttonNames.back", "Back"));
		gui.setItem(15, backButton);

		if (isArchived) {
			ItemStack archiveButton = createCustomPlayerHead(guiTextures.archiveTexture, ChatColor.YELLOW + BugReportLanguage.getValueFromLanguageFile("buttonNames.archive", "Archive"), 16);
			gui.setItem(11, archiveButton);
		} else {
			ItemStack deleteButton = createCustomPlayerHead(guiTextures.deleteTexture, ChatColor.YELLOW + BugReportLanguage.getValueFromLanguageFile("buttonNames.delete", "Delete"), 18);
			gui.setItem(11, deleteButton);
		}

		return gui;
	}

	public void archiveReport(@NotNull Player player, @NotNull Integer reportIDGUI, @NotNull Boolean isArchivedDetails) {
		BugReportDatabase.updateBugReportArchive(reportIDGUI, 1);
		player.openInventory(isArchivedDetails ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));
		player.sendMessage(returnStartingMessage(ChatColor.RED)
				+ " Bug Report #" + reportIDGUI + " has been archived.");
	}

	public void deleteReport(@NotNull Player player, @NotNull Integer reportIDGUI, @NotNull Boolean isArchivedDetails) {
		UUID playerId = player.getUniqueId();
		BugReportDatabase.deleteBugReport(reportIDGUI);

		List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
		reports.removeIf(report -> report.contains("Report ID: " + reportIDGUI));
		bugReports.put(playerId, reports);

		player.openInventory(isArchivedDetails ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));
		player.sendMessage(returnStartingMessage(ChatColor.RED)
				+ " Bug Report #" + reportIDGUI + " has been deleted.");
	}

	public record BugReportConfirmationListener(Inventory gui, Integer reportIDGUI,
												Boolean fromArchivedGUI) implements Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String displayName = event.getView().getTitle();
			if (debugMode) {
				plugin.getLogger().info("Clicked inventory: " + displayName);
			}

			String customDisplayName = getKeyFromTranslation(displayName);
			if (customDisplayName == null || customDisplayName.equals(" ")) {
				return;
			}

			boolean isArchivedDetails = customDisplayName.equals("buttonNames.confirmationArchive");
			boolean isDeletedDetails = customDisplayName.equals("buttonNames.confirmationDelete");

			if (!isArchivedDetails && !isDeletedDetails) {
				plugin.getLogger().severe("Something went wrong with the languages folder. Please remove the file and restart the server.");
				plugin.getLogger().severe("If the issue persists, please contact the developer.");
				logErrorMessage("Something went wrong with the languages folder.");
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

			String itemDisplayName = itemMeta.getDisplayName();
			String customItemDisplayName = getKeyFromTranslation(itemDisplayName);
			if (customItemDisplayName == null || customItemDisplayName.equals(" ")) {
				return;
			}

			if (debugMode) {
				plugin.getLogger().info("Clicked item: " + customItemDisplayName);
			}

			if (isArchivedDetails) {
				if (debugMode) {
					plugin.getLogger().info("Opening archived confirmation GUI.");
				}
				switch (customItemDisplayName) {
					case "buttonNames.archive" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Archiving report: " + reportIDGUI);
						}
						new BugReportConfirmationGUI().archiveReport(player, reportIDGUI, true);

						player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));

						HandlerList.unregisterAll(this);
					}
					case "buttonNames.back" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Going back to bug reports.");
						}
						returnFromConfirmationGUI(player, false);
					}
				}
			}

			if (isDeletedDetails) {
				if (debugMode) {
					plugin.getLogger().info("Opening delete confirmation GUI.");
				}

				switch (Objects.requireNonNull(customItemDisplayName)) {
					case "buttonNames.delete" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Deleting report: " + reportIDGUI);
						}
						new BugReportConfirmationGUI().deleteReport(player, reportIDGUI, isArchivedDetails);

						player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));

						HandlerList.unregisterAll(this);
					}
					case "buttonNames.back" -> {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Going back to archived reports.");
						}
						returnFromConfirmationGUI(player, false);
					}
				}
			}
		}

		private void returnFromConfirmationGUI(@NotNull Player player, Boolean fromArchivedGUI) {
			player.openInventory(fromArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));

			List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
			String report = reports.stream().filter(reportString -> reportString.contains("Report ID: " + reportIDGUI)).findFirst().orElse(null);

			openBugReportDetailsGUI(player, report, reportIDGUI, fromArchivedGUI);

			HandlerList.unregisterAll(this);
		}
	}
}
