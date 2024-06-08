package com.bugreportmc.bugreport.gui

import com.bugreportmc.bugreport.BugReportDatabase.Companion.deleteBugReport
import com.bugreportmc.bugreport.BugReportDatabase.Companion.getStaticUUID
import com.bugreportmc.bugreport.BugReportDatabase.Companion.updateBugReportArchive
import com.bugreportmc.bugreport.BugReportLanguage
import com.bugreportmc.bugreport.BugReportManager.Companion.bugReports
import com.bugreportmc.bugreport.BugReportManager.Companion.createButton
import com.bugreportmc.bugreport.BugReportManager.Companion.debugMode
import com.bugreportmc.bugreport.BugReportManager.Companion.getArchivedBugReportsGUI
import com.bugreportmc.bugreport.BugReportManager.Companion.getBugReportGUI
import com.bugreportmc.bugreport.BugReportManager.Companion.localCurrentPage
import com.bugreportmc.bugreport.BugReportManager.Companion.playButtonClickSound
import com.bugreportmc.bugreport.BugReportManager.Companion.returnStartingMessage
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.BugReportSettings
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.keys.guiTextures
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

class BugReportConfirmationGUI {
	fun archiveReport(player: Player, reportIDGUI: Int, isArchivedDetails: Boolean) {
		updateBugReportArchive(reportIDGUI, 1)
		player.openInventory(
			if (isArchivedDetails) {
				getArchivedBugReportsGUI(
					localCurrentPage, player
				)
			} else getBugReportGUI(
				localCurrentPage, player
			)
		)
		player.sendMessage(
			returnStartingMessage(ChatColor.RED) + " Bug Report #" + reportIDGUI + " has been archived."
		)
	}

	fun deleteReport(player: Player, reportIDGUI: Int, isArchivedDetails: Boolean) {
		deleteBugReport(reportIDGUI)

		val reports = bugReports.getOrDefault(
			getStaticUUID(), ArrayList<String>(listOf("DUMMY"))
		)
		reports.removeIf { report: String -> report.contains("Report ID: $reportIDGUI") }
		bugReports = bugReports.toMutableMap().apply { put(getStaticUUID(), reports) }

		player.openInventory(
			if (isArchivedDetails) getArchivedBugReportsGUI(
				localCurrentPage, player
			) else getBugReportGUI(
				localCurrentPage, player
			)
		)
		player.sendMessage(
			returnStartingMessage(ChatColor.RED) + " Bug Report #" + reportIDGUI + " has been deleted."
		)
	}

	data class BugReportConfirmationListener(
		val gui: Inventory, val reportIDGUI: Int, val fromArchivedGUI: Boolean,
	) : Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		fun onInventoryClick(event: InventoryClickEvent) {
			val displayName = ChatColor.stripColor(event.view.title)

			if (debugMode) {
				plugin.logger.info("Clicked inventory: $displayName")
			}

			val customDisplayName = BugReportLanguage.getEnglishValueFromValue(displayName)
			val isArchivedDetails = customDisplayName!!.startsWith("Archive Bug Report")
			val isDeletedDetails = customDisplayName.startsWith("Delete Bug Report")

			if (!isArchivedDetails && !isDeletedDetails) {
				plugin.logger.warning("Something went wrong with the languages folder. Please remove the file and restart the server.")
				plugin.logger.warning("If the issue persists, please contact the developer.")
				logErrorMessage("Something went wrong with the languages folder.")
				return
			}

			event.isCancelled = true

			val player = event.whoClicked as Player

			val clickedItem = event.currentItem
			if (clickedItem == null || clickedItem.type == Material.AIR) {
				return
			}

			val itemMeta = clickedItem.itemMeta
			if (itemMeta == null || !itemMeta.hasDisplayName()) {
				return
			}

			val itemDisplayName = itemMeta.displayName
			val customItemDisplayName =
				BugReportLanguage.getEnglishValueFromValue(ChatColor.stripColor(itemDisplayName))

			if (debugMode) {
				plugin.logger.info("Clicked item: $customItemDisplayName")
			}

			if (isArchivedDetails) {
				if (debugMode) {
					plugin.logger.info("Opening archived confirmation GUI.")
				}
				when (customItemDisplayName) {
					"Archive" -> {
						playButtonClickSound(player)

						if (debugMode) {
							plugin.logger.info("Archiving report: $reportIDGUI")
						}
						BugReportConfirmationGUI().archiveReport(player, reportIDGUI, true)

						player.openInventory(
							if (fromArchivedGUI) getArchivedBugReportsGUI(
								localCurrentPage, player
							) else getBugReportGUI(
								localCurrentPage, player
							)
						)

						HandlerList.unregisterAll(this)
					}

					"Back" -> {
						playButtonClickSound(player)

						if (debugMode) {
							plugin.logger.info("Going back to bug reports.")
						}
						returnFromConfirmationGUI(player, false)
					}
				}
			}

			if (isDeletedDetails) {
				if (debugMode) {
					plugin.logger.info("Opening delete confirmation GUI.")
				}
				when (customItemDisplayName) {
					"Delete" -> {
						playButtonClickSound(player)

						if (debugMode) {
							plugin.logger.info("Deleting report: $reportIDGUI")
						}
						BugReportConfirmationGUI().deleteReport(player, reportIDGUI, isArchivedDetails)

						player.openInventory(
							if (fromArchivedGUI) getArchivedBugReportsGUI(
								localCurrentPage, player
							) else getBugReportGUI(
								localCurrentPage, player
							)
						)

						HandlerList.unregisterAll(this)
					}

					"Back" -> {
						playButtonClickSound(player)

						if (debugMode) {
							plugin.logger.info("Going back to archived reports.")
						}
						returnFromConfirmationGUI(player, false)
					}
				}
			}
		}

		private fun returnFromConfirmationGUI(player: Player, fromArchivedGUI: Boolean) {
			player.openInventory(
				if (fromArchivedGUI) getArchivedBugReportsGUI(
					localCurrentPage, player
				) else getBugReportGUI(
					localCurrentPage, player
				)
			)

			val reports: List<String> = bugReports.getOrDefault(
				getStaticUUID(), ArrayList<String>(listOf("DUMMY"))
			)
			val report =
				reports.stream().filter { reportString: String? -> reportString!!.contains("Report ID: $reportIDGUI") }
					.findFirst().orElse(null)

			bugreportGUI.openBugReportDetailsGUI(player, report, reportIDGUI, fromArchivedGUI)

			HandlerList.unregisterAll(this)
		}
	}

	companion object {
		fun openConfirmationGUI(player: Player, isArchived: Boolean, bugReportID: String?) {
			player.openInventory(getConfirmationGUI(isArchived, bugReportID))
		}

		private fun getConfirmationGUI(isArchived: Boolean, bugReportID: String?): Inventory {
			val guiTitle: String

			if (isArchived) {
				guiTitle = ChatColor.YELLOW.toString() + Objects.requireNonNull(
					BugReportLanguage.getValueFromLanguageFile(
						"buttonNames.confirmationArchive", "Archive Bug Report?"
					)
				)
			} else {
				guiTitle = ChatColor.YELLOW.toString() + BugReportLanguage.getValueFromLanguageFile(
					"buttonNames.confirmationDelete", "Delete Bug Report?"
				)
			}

			val gui = Bukkit.createInventory(null, 27, guiTitle)
			val backButton: ItemStack = createButton(
				Material.BARRIER,
				ChatColor.YELLOW.toString() + BugReportLanguage.getValueFromLanguageFile("buttonNames.back", "Back")
			)
			gui.setItem(15, backButton)

			if (isArchived) {
				val archiveButton = BugReportSettings.createCustomPlayerHead(
					guiTextures.archiveTexture,
					ChatColor.YELLOW.toString() + BugReportLanguage.getValueFromLanguageFile(
						"buttonNames.archive", "Archive"
					),
					16
				)
				gui.setItem(11, archiveButton)
			} else {
				val deleteButton = BugReportSettings.createCustomPlayerHead(
					guiTextures.deleteTexture, ChatColor.YELLOW.toString() + BugReportLanguage.getValueFromLanguageFile(
						"buttonNames.delete", "Delete"
					), 18
				)
				gui.setItem(11, deleteButton)
			}

			return gui
		}
	}
}
