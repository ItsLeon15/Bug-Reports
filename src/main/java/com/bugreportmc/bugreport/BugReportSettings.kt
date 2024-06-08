package com.bugreportmc.bugreport

import com.bugreportmc.bugreport.BugReportDatabase.Companion.getStaticUUID
import com.bugreportmc.bugreport.BugReportDatabase.Companion.updateReportStatus
import com.bugreportmc.bugreport.BugReportLanguage.Companion.getEnglishValueFromValue
import com.bugreportmc.bugreport.BugReportLanguage.Companion.getValueFromLanguageFile
import com.bugreportmc.bugreport.BugReportLanguage.Companion.setPluginLanguage
import com.bugreportmc.bugreport.BugReportManager.Companion.bugReports
import com.bugreportmc.bugreport.BugReportManager.Companion.checkForKey
import com.bugreportmc.bugreport.BugReportManager.Companion.config
import com.bugreportmc.bugreport.BugReportManager.Companion.createButton
import com.bugreportmc.bugreport.BugReportManager.Companion.debugMode
import com.bugreportmc.bugreport.BugReportManager.Companion.endingPluginTitleColor
import com.bugreportmc.bugreport.BugReportManager.Companion.playButtonClickSound
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginColor
import com.bugreportmc.bugreport.BugReportManager.Companion.reloadConfig
import com.bugreportmc.bugreport.BugReportManager.Companion.returnStartingMessage
import com.bugreportmc.bugreport.BugReportManager.Companion.saveConfig
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.commands.BugReportCommand
import com.bugreportmc.bugreport.gui.bugreportGUI
import com.bugreportmc.bugreport.keys.guiTextures
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.net.URL
import java.util.*
import java.util.function.Consumer
import java.util.stream.IntStream

object BugReportSettings {
	private var newReportIDGUI: Int? = null

	private fun setBorder(gui: Inventory, borderMaterial: Material) {
		IntStream.range(0, 9).forEach { i: Int -> gui.setItem(i, createButton(borderMaterial, " ")) }
		IntStream.range(36, 45).forEach { i: Int -> gui.setItem(i, createButton(borderMaterial, " ")) }
		IntStream.range(9, 36).filter { i: Int -> i % 9 == 0 || i % 9 == 8 }
			.forEach { i: Int -> gui.setItem(i, createButton(borderMaterial, " ")) }
	}

	fun getSettingsGUI(): Inventory {
		val gui = Bukkit.createInventory(
			null,
			45,
			ChatColor.YELLOW.toString() + "Bug Report - " + getValueFromLanguageFile("buttonNames.settings", "Settings")
		)

		val setDiscordWebhook = createCustomPlayerHead(
			guiTextures.setDiscordWebhookTexture,
			getValueFromLanguageFile("buttonNames.enableDiscordWebhook", "Enable Discord Webhook"),
			1
		)
		val setLanguage = createCustomPlayerHead(
			guiTextures.setLanguageTexture, getValueFromLanguageFile("buttonNames.setLanguage", "Set Language"), 2
		)

		val maxReportsPerPlayer = createButton(
			Material.PAPER, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
				"buttonNames.setMaxReportsPerPlayer", "Set Max Reports Per Player"
			)
		)
		val toggleCategorySelection = createButton(
			Material.CHEST, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
				"buttonNames.enableCategorySelection", "Enable Category Selection"
			)
		)
		val setBugReportNotifications = createButton(
			Material.BELL, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
				"buttonNames.enableBugReportNotifications", "Enable Bug Report Notifications"
			)
		)
		val onIcon = createButton(
			Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
		)
		val offIcon = createButton(
			Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
		)
		val otherSettings = createButton(
			Material.BOOK,
			ChatColor.YELLOW.toString() + getValueFromLanguageFile("buttonNames.otherSettings", "Other Settings")
		)
		val viewAllStatus = createButton(
			Material.BOOKSHELF,
			ChatColor.YELLOW.toString() + getValueFromLanguageFile("buttonNames.viewStatus", "View Status")
		)

		setBorder(gui, Material.GRAY_STAINED_GLASS_PANE)

		gui.setItem(10, setDiscordWebhook)
		gui.setItem(11, setBugReportNotifications)
		gui.setItem(12, toggleCategorySelection)
		gui.setItem(13, maxReportsPerPlayer)
		gui.setItem(14, setLanguage)
		gui.setItem(15, otherSettings)
		gui.setItem(16, viewAllStatus)

		gui.setItem(19, if (getDiscordWebhookToggle()) onIcon else offIcon)
		gui.setItem(20, if (getBugReportNotificationsToggle()) onIcon else offIcon)
		gui.setItem(21, if (getCategorySelectionToggle()) onIcon else offIcon)

		gui.setItem(
			40, createButton(
				Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.close", "Close")
			)
		)

		return gui
	}

	private fun getDiscordWebhookToggle(): Boolean {
		return config.getBoolean("enableDiscordWebhook")
	}

	private fun getBugReportNotificationsToggle(): Boolean {
		return config.getBoolean("enableBugReportNotifications")
	}

	private fun getCategorySelectionToggle(): Boolean {
		return config.getBoolean("enablePluginReportCategoriesGUI")
	}

	private fun setDiscordWebhookToggle(player: Player) {
		playButtonClickSound(player)

		if (debugMode) {
			plugin.logger.info("Discord Webhook toggle clicked by " + player.name)
		}
		val toggle = getDiscordWebhookToggle()
		config.set("enableDiscordWebhook", !toggle)
		saveConfig()
		player.openInventory.setItem(
			19, if (getDiscordWebhookToggle()) createButton(
				Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
			)
			else createButton(
				Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
			)
		)
	}

	private fun setBugReportNotificationsToggle(player: Player) {
		playButtonClickSound(player)

		if (debugMode) {
			plugin.logger.info("Bug Report Notifications toggle clicked by " + player.name)
		}
		val toggle = getBugReportNotificationsToggle()

		config.set("enableBugReportNotifications", !toggle)
		saveConfig()

		player.openInventory.setItem(
			20, if (toggle) createButton(
				Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
			)
			else createButton(
				Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
			)
		)
	}

	private fun setCategorySelectionToggle(player: Player) {
		playButtonClickSound(player)

		if (debugMode) {
			plugin.logger.info("Category Selection toggle clicked by " + player.name)
		}
		val toggle = getCategorySelectionToggle()
		config.set("enablePluginReportCategoriesGUI", !toggle)
		saveConfig()
		player.openInventory.setItem(
			21, if (toggle) createButton(
				Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
			)
			else createButton(
				Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
			)
		)
	}

	private fun setLanguageToggle(player: Player) {
		playButtonClickSound(player)

		if (debugMode) {
			plugin.logger.info("Language toggle clicked by " + player.name)
		}
		player.openInventory(openLanguageGUI())
	}

	private fun openLanguageGUI(): Inventory {
		val gui = Bukkit.createInventory(
			null, 45, ChatColor.YELLOW.toString() + "Bug Report - " + getValueFromLanguageFile(
				"buttonNames.language", "Language"
			)
		)

		setBorder(gui, Material.GRAY_STAINED_GLASS_PANE)

		gui.setItem(10, createCustomPlayerHead(guiTextures.EnglishTexture, "English", 11))
		gui.setItem(11, createCustomPlayerHead(guiTextures.FrenchTexture, "French", 12))
		gui.setItem(12, createCustomPlayerHead(guiTextures.GermanTexture, "German", 13))
		gui.setItem(13, createCustomPlayerHead(guiTextures.SpanishTexture, "Spanish", 14))
		gui.setItem(14, createCustomPlayerHead(guiTextures.ItalianTexture, "Italian", 15))
		gui.setItem(15, createCustomPlayerHead(guiTextures.SimplifiedChineseTexture, "Simplified Chinese", 16))
		gui.setItem(16, createCustomPlayerHead(guiTextures.RussianTexture, "Russian", 17))

		val language: String = config.getString("language").toString()

		for (i in 19..25) {
			gui.setItem(
				i, createButton(
					Material.GRAY_DYE, getValueFromLanguageFile("buttonNames.false", "Off")
				)
			)
		}

		when (Objects.requireNonNull(language)) {
			"en_US" -> gui.setItem(
				28 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)

			"fr_FR" -> gui.setItem(
				29 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)

			"de_DE" -> gui.setItem(
				30 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)

			"es_ES" -> gui.setItem(
				31 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)

			"it_IT" -> gui.setItem(
				32 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)

			"zh_CN" -> gui.setItem(
				33 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)

			"ru_RU" -> gui.setItem(
				34 - 9, createButton(
					Material.LIME_DYE, getValueFromLanguageFile("buttonNames.true", "On")
				)
			)
		}
		gui.setItem(
			40, createButton(
				Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.back", "Back")
			)
		)

		return gui
	}

	fun createCustomPlayerHead(texture: String, name: String, modelData: Int): ItemStack {
		if (debugMode) {
			plugin.logger.info("Creating custom player head with texture: $texture, name: $name, modelData: $modelData")
		}
		return createCustomPlayerHead(texture, name, modelData, null)
	}

	fun createCustomPlayerHead(texture: String, name: String, modelData: Int, nameColor: ChatColor?): ItemStack {
		if (debugMode) {
			plugin.logger.info("Creating custom player head with texture: $texture, name: $name, modelData: $modelData, nameColor: $nameColor")
		}
		val playerHead = ItemStack(Material.PLAYER_HEAD)
		val skullMeta = playerHead.itemMeta as SkullMeta?

		if (skullMeta != null) {
			try {
				val decodedValue = String(Base64.getDecoder().decode(texture))
				val textureJson = JsonParser.parseString(decodedValue).asJsonObject
				val textureUrl = textureJson.getAsJsonObject("textures").getAsJsonObject("SKIN")["url"].asString

				if (debugMode) {
					plugin.logger.info("Texture URL: $textureUrl")
				}

				val profile = Bukkit.createPlayerProfile(UUID.randomUUID())
				val textures = profile.textures
				textures.skin = URL(textureUrl)
				profile.setTextures(textures)

				skullMeta.ownerProfile = profile
				skullMeta.setDisplayName((nameColor ?: ChatColor.YELLOW).toString() + name)
				skullMeta.setCustomModelData(modelData)
				playerHead.setItemMeta(skullMeta)

				if (debugMode) {
					plugin.logger.info("Custom player head created successfully.")
				}
			} catch (e: Exception) {
				plugin.logger.warning("Failed to create custom player head: " + e.message)
				logErrorMessage("Failed to create custom player head: " + e.message)
				return ItemStack(Material.PLAYER_HEAD)
			}
		}

		return playerHead
	}

	fun getStatusSelectionGUI(reportIDGUI: Int?): Inventory {
		newReportIDGUI = reportIDGUI
		val gui = Bukkit.createInventory(
			null, 45, ChatColor.YELLOW.toString() + "Bug Report - " + getValueFromLanguageFile(
				"buttonNames.statusSelection", "Status Selection"
			)
		)

		setBorder(gui, Material.GRAY_STAINED_GLASS_PANE)

		val statuses: List<Map<*, *>> = config.getMapList("statuses")
		for (statusMap in statuses) {
			val statusName = statusMap["name"] as String?
			val statusDescription = statusMap["description"] as String?

			ChatColor.valueOf(statusMap["color"].toString().uppercase(Locale.getDefault()))
			val newStatusColor = ChatColor.valueOf((statusMap["color"] as String?)!!.uppercase(Locale.getDefault()))

			val newStatusIcon =
				if (Material.matchMaterial((statusMap["icon"] as String?)!!) != null) (statusMap["icon"] as String?)?.let {
					Material.matchMaterial(
						it
					)
				} else {
					Material.BARRIER
				}

			val statusItem = newStatusIcon?.let { createButton(it, newStatusColor.toString() + statusName) }!!
			val statusItemMeta = statusItem.itemMeta
			if (statusItemMeta != null) {
				statusItemMeta.lore = listOf(ChatColor.GRAY.toString() + statusDescription)
				statusItem.setItemMeta(statusItemMeta)
			}

			gui.addItem(statusItem)
		}

		gui.setItem(
			40, createButton(
				Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.back", "Back")
			)
		)

		return gui
	}

	class BugReportSettingsListener : Listener {
		private val setMaxReportsClickMap: MutableMap<UUID, String?> = HashMap()
		private val setReportCooldownClickMap: MutableMap<UUID, String?> = HashMap()
		private val setNewNameClickMap: MutableMap<UUID, String?> = HashMap()
		private val removeStatusClickMap: MutableMap<UUID, String?> = HashMap()
		private val renameStatusClickMap: MutableMap<UUID, String?> = HashMap()
		private val setNewMaterialStatusClickMap: MutableMap<UUID, String?> = HashMap()
		private val setNewColorStatusClickMap: MutableMap<UUID, String?> = HashMap()
		private val setNewDescriptionStatusClickMap: MutableMap<UUID, String?> = HashMap()

		private var savedStatusName: String? = ""
		private var savedStatusID: Int? = 0

		@EventHandler(priority = EventPriority.NORMAL)
		fun onInventoryClick(event: InventoryClickEvent) {
			var displayName = ChatColor.stripColor(event.view.title)

			if (displayName!!.contains("Bug Report - ")) {
				displayName = displayName.substring(13)
			}

			if (debugMode) {
				plugin.logger.info("Clicked inventory: $displayName")
			}

			val customDisplayName: String = getEnglishValueFromValue(displayName).toString()

			if (customDisplayName.contains("Status Selection")) {
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
				val customItemDisplayName = ChatColor.stripColor(itemDisplayName)
				val englishItemDisplayName: String =
					getEnglishValueFromValue(customItemDisplayName).toString()

				if (englishItemDisplayName == "Back") {
					playButtonClickSound(player)

					val reports: List<String> = bugReports.getOrDefault(
						getStaticUUID(), ArrayList<String>(
							listOf("DUMMY")
						)
					)
					val report = reports.stream()
						.filter { reportString: String? -> reportString!!.contains("Report ID: $newReportIDGUI") }
						.findFirst().orElse(null)
					val fromArchivedGUI =
						report != null && report.contains("Archived") && report.contains("Archived: 1")

					newReportIDGUI?.let { bugreportGUI.openBugReportDetailsGUI(player, report, it, fromArchivedGUI) }
					return
				}

				val statuses: List<Map<*, *>> = config.getMapList("statuses")
				for (statusMap in statuses) {
					val statusName = statusMap["name"] as String?
					if (statusName == customItemDisplayName) {
						playButtonClickSound(player)

						val statusID = statusMap["id"] as Int?
						newReportIDGUI?.let {
							if (statusID != null) {
								updateReportStatus(it, statusID)
							}
						}

						player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "The status of the report has been updated to " + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ".")
						player.closeInventory()
					}
				}
			}

			if (customDisplayName.contains("Settings")) {
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
				val customItemDisplayName: String =
					getEnglishValueFromValue(itemDisplayName).toString()

				if (customItemDisplayName == "Close") {
					player.closeInventory()
					return
				}

				if (clickedItem.itemMeta!!.hasCustomModelData()) {
					if (clickedItem.itemMeta!!.customModelData == 1) {
						setDiscordWebhookToggle(player)
					} else if (clickedItem.itemMeta!!.customModelData == 2) {
						setLanguageToggle(player)
					}
				}

				when (customItemDisplayName) {
					"Enable Bug Report Notifications" -> setBugReportNotificationsToggle(player)
					"Enable Category Selection" -> setCategorySelectionToggle(player)
					"Set Max Reports Per Player" -> {
						playButtonClickSound(player)

						player.closeInventory()
						if (config.getBoolean("useTitleInsteadOfMessage")) {
							player.sendTitle(
								returnStartingMessage(ChatColor.YELLOW), getValueFromLanguageFile(
									"enterMaxReportsPerPlayer",
									"Enter the max reports a player can submit. Or type 'cancel' to cancel"
								), 10, 70, 20
							)
						} else {
							player.sendMessage(
								returnStartingMessage(ChatColor.YELLOW) + getValueFromLanguageFile(
									"enterMaxReportsPerPlayer",
									"Enter the max reports a player can submit. Or type 'cancel' to cancel"
								)
							)
						}
						setMaxReportsClickMap[player.uniqueId] = true.toString()
						setMaxReportsClickMap[player.uniqueId] = customItemDisplayName
					}

					"Set Report Cooldown" -> {
						player.closeInventory()
						player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "Enter the cooldown between reports in seconds. Or type 'cancel' to cancel.")

						setReportCooldownClickMap[player.uniqueId] = true.toString()
						setReportCooldownClickMap[player.uniqueId] = customItemDisplayName
					}

					"Other Settings" -> {
						playButtonClickSound(player)
						player.openInventory(otherSettingsGUI)
					}

					"View Status" -> {
						playButtonClickSound(player)
						player.openInventory(viewStatusGUI)
					}
				}
			}

			if (customDisplayName.contains("Language")) {
				event.isCancelled = true

				val player = event.whoClicked as Player
				val clickedInventory = event.clickedInventory

				val clickedItem = event.currentItem
				if (clickedInventory == null || clickedItem == null || clickedItem.type == Material.AIR) {
					return
				}

				val itemMeta = clickedItem.itemMeta
				if (itemMeta == null || !itemMeta.hasDisplayName()) {
					return
				}

				val itemDisplayName = itemMeta.displayName
				val customItemDisplayName: String =
					getEnglishValueFromValue(itemDisplayName).toString()
				if (customItemDisplayName == "Back") {
					playButtonClickSound(player)

					player.openInventory(getSettingsGUI())
					return
				}

				if (clickedItem.itemMeta!!.hasCustomModelData()) {
					val customModelData = clickedItem.itemMeta!!.customModelData
					playButtonClickSound(player)

					when (customModelData) {
						11 -> setLanguage("en_US", "English", player)
						12 -> setLanguage("fr_FR", "French", player)
						13 -> setLanguage("de_DE", "German", player)
						14 -> setLanguage("es_ES", "Spanish", player)
						15 -> setLanguage("it_IT", "Italian", player)
						16 -> setLanguage("zh_CN", "Simplified Chinese", player)
						17 -> setLanguage("ru_RU", "Russian", player)
					}
				}
			}

			if (customDisplayName.contains("Other Settings")) {
				event.isCancelled = true

				val player = event.whoClicked as Player
				val clickedInventory = event.clickedInventory
				val clickedItem = event.currentItem

				if (clickedInventory == null || clickedItem == null || clickedItem.type == Material.AIR) {
					return
				}

				val itemMeta = clickedItem.itemMeta

				if (itemMeta == null || !itemMeta.hasDisplayName()) {
					return
				}

				val itemDisplayName = itemMeta.displayName
				val customItemDisplayName: String =
					getEnglishValueFromValue(itemDisplayName).toString()

				if (customItemDisplayName == "Back") {
					playButtonClickSound(player)

					player.openInventory(getSettingsGUI())
					return
				}

				when (customItemDisplayName) {
					"Enable Title Message" -> setTitleMessage(player)
					"Enable Player Heads" -> setPlayerHead(player)
					"Enable Report Book" -> setReportBook(player)
				}
			}

			if (customDisplayName.contains("View Status")) {
				event.isCancelled = true

				val player = event.whoClicked as Player
				val clickedInventory = event.clickedInventory
				val clickedItem = event.currentItem

				if (clickedInventory == null || clickedItem == null || clickedItem.type == Material.AIR) {
					return
				}

				val itemMeta = clickedItem.itemMeta

				if (itemMeta == null || !itemMeta.hasDisplayName()) {
					return
				}

				val itemDisplayName = itemMeta.displayName
				val customItemDisplayName: String =
					getEnglishValueFromValue(itemDisplayName).toString()

				if (customItemDisplayName == "Back") {
					playButtonClickSound(player)
					player.openInventory(getSettingsGUI())
					return
				}

				if (customItemDisplayName == " ") {
					return
				}

				val statuses: List<Map<*, *>> = config.getMapList("statuses")
				for (statusMap in statuses) {
					val statusName = statusMap["name"] as String?
					val statusID = statusMap["id"] as Int?

					if (statusName == customItemDisplayName) {
						playButtonClickSound(player)

						savedStatusName = statusName
						savedStatusID = statusID

						player.openInventory(getStatusInfoGUI(statusMap))
					}
				}
			}

			if (customDisplayName.contains("Edit Status")) {
				event.isCancelled = true

				val player = event.whoClicked as Player
				val clickedInventory = event.clickedInventory
				val clickedItem = event.currentItem

				if (clickedInventory == null || clickedItem == null || clickedItem.type == Material.AIR) {
					return
				}

				val itemMeta = clickedItem.itemMeta

				if (itemMeta == null || !itemMeta.hasDisplayName()) {
					return
				}

				val itemDisplayName = itemMeta.displayName
				val customItemDisplayName: String =
					getEnglishValueFromValue(itemDisplayName).toString()

				when (customItemDisplayName) {
					"Back" -> {
						playButtonClickSound(player)
						player.openInventory(viewStatusGUI)
						return
					}

					" " -> {
						return
					}

					"Delete" -> {
						val statuses: List<Map<*, *>> = config.getMapList("statuses")
						for (statusMap in statuses) {
							val statusName = statusMap["name"] as String?
							if (statusMap["id"] == savedStatusID) {
								playButtonClickSound(player)

								player.closeInventory()
								player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "Type 'confirm' to delete the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel.")

								removeStatusClickMap[player.uniqueId] = true.toString()
								removeStatusClickMap[player.uniqueId] = statusName
							}
						}
					}
				}
				if (customItemDisplayName == "Status Name") {
					val statuses: List<Map<*, *>> = config.getMapList("statuses")
					for (statusMap in statuses) {
						val statusName = statusMap["name"] as String?
						if (statusMap["id"] == savedStatusID) {
							playButtonClickSound(player)

							player.closeInventory()
							player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "Type the new name for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel.")

							renameStatusClickMap[player.uniqueId] = true.toString()
							renameStatusClickMap[player.uniqueId] = statusName
						}
					}
				}

				if (customItemDisplayName == "Status Material") {
					val statuses: List<Map<*, *>> = config.getMapList("statuses")
					for (statusMap in statuses) {
						val statusName = statusMap["name"] as String?
						if (statusMap["id"] == savedStatusID) {
							playButtonClickSound(player)
							player.closeInventory()

							player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "Type the new material for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel.")
							setNewMaterialStatusClickMap[player.uniqueId] = true.toString()
							setNewMaterialStatusClickMap[player.uniqueId] = statusName
						}
					}
				}

				if (customItemDisplayName == "Status Color") {
					val statuses: List<Map<*, *>> = config.getMapList("statuses")
					for (statusMap in statuses) {
						val statusName = statusMap["name"] as String?
						if (statusMap["id"] == savedStatusID) {
							playButtonClickSound(player)

							player.closeInventory()
							player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "Type the new color for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel.")

							setNewColorStatusClickMap[player.uniqueId] = true.toString()
							setNewColorStatusClickMap[player.uniqueId] = statusName
						}
					}
				}

				if (customItemDisplayName == "Status Description") {
					val statuses: List<Map<*, *>> = config.getMapList("statuses")
					for (statusMap in statuses) {
						val statusName = statusMap["name"] as String?

						if (statusMap["id"] == savedStatusID) {
							playButtonClickSound(player)

							player.closeInventory()
							player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + "Type the new description for the status (" + ChatColor.BOLD + statusName + ChatColor.RESET + pluginColor + ") or 'cancel' to cancel.")

							setNewDescriptionStatusClickMap[player.uniqueId] = true.toString()
							setNewDescriptionStatusClickMap[player.uniqueId] = statusName
						}
					}
				}
			}
		}

		private val viewStatusGUI: Inventory
			get() {
				val gui = Bukkit.createInventory(
					null,
					45,
					Objects.requireNonNullElse<ChatColor>(endingPluginTitleColor, ChatColor.YELLOW)
						.toString() + "Bug Report - " + getValueFromLanguageFile(
						"buttonNames.viewStatus", "View Status"
					)
				)

				setBorder(gui, Material.GRAY_STAINED_GLASS_PANE)

				val statuses: List<Map<*, *>> = config.getMapList("statuses")

				var statusIndex = 0
				for (i in 9..35) {
					if (i % 9 != 0 && i % 9 != 8) {
						if (statusIndex < statuses.size) {
							val statusMap = statuses[statusIndex]
							val statusName = statusMap["name"] as String?

							val statusColorString = (statusMap["color"] as String?)!!.uppercase(Locale.getDefault())
							val statusColor = ChatColor.valueOf(statusColorString)
							val statusIcon =
								if (Material.matchMaterial((statusMap["icon"] as String?)!!) != null) Material.matchMaterial(
									(statusMap["icon"] as String?)!!
								) else Material.BARRIER

							val statusItem = statusIcon?.let { createButton(it, statusColor.toString() + statusName) }!!
							val statusMeta = statusItem.itemMeta

							val statusLore: MutableList<String> = ArrayList()
							statusLore.add(ChatColor.GRAY.toString() + "Click to edit the status (" + statusColor + statusName + ChatColor.GRAY + ")")
							Objects.requireNonNull(statusMeta)?.lore = statusLore
							statusItem.setItemMeta(statusMeta)

							gui.setItem(i, statusItem)
							statusIndex++
						}
					}
				}

				gui.setItem(
					40, createButton(
						Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile(
							"buttonNames.back", "Back"
						)
					)
				)

				return gui
			}

		private fun getStatusInfoGUI(statusMap: Map<*, *>): Inventory {
			val gui = Bukkit.createInventory(
				null, 45, ChatColor.YELLOW.toString() + "Bug Report - " + getValueFromLanguageFile(
					"buttonNames.editStatus", "Edit Status"
				)
			)

			val itemStackMaterial = getMaterialFromMap(statusMap, "icon", Material.BARRIER)
			val itemStackColor = getChatColorFromMap(statusMap, "color", ChatColor.WHITE)

			setBorder(gui, getStainedGlassPaneColor(statusMap))

			val statusName: ItemStack = createButton(
				Material.NAME_TAG, itemStackColor.toString() + getValueFromLanguageFile(
					"buttonNames.statusName", "Status Name"
				)
			)
			val statusMaterial = itemStackMaterial?.let {
				createButton(
					it, itemStackColor.toString() + getValueFromLanguageFile(
						"buttonNames.statusMaterial", "Status Material"
					)
				)
			}
			val statusColor: ItemStack = createButton(
				Material.PAINTING, itemStackColor.toString() + getValueFromLanguageFile(
					"buttonNames.statusColor", "Status Color"
				)
			)
			val statusDescription: ItemStack = createButton(
				Material.BOOK, itemStackColor.toString() + getValueFromLanguageFile(
					"buttonNames.statusDescription", "Status Description"
				)
			)
			val deleteStatus = createCustomPlayerHead(
				guiTextures.deleteStatusTexture, getValueFromLanguageFile("buttonNames.delete", "Delete"), 1
			)

			setItemMeta(statusName, statusMap, "name")
			if (statusMaterial != null) {
				setItemMeta(statusMaterial, statusMap, "icon")
			}
			setItemMeta(statusColor, statusMap, "color")
			setItemMeta(statusDescription, statusMap, "description")

			gui.setItem(20, statusName)
			gui.setItem(21, statusColor)
			gui.setItem(22, statusDescription)
			gui.setItem(23, statusMaterial)
			gui.setItem(
				40, createButton(
					Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.back", "Back")
				)
			)
			gui.setItem(42, deleteStatus)

			return gui
		}

		private fun getMaterialFromMap(map: Map<*, *>, key: String, defaultMaterial: Material): Material? {
			val value = map[key]
			return if (value != null) Material.matchMaterial((value as String?)!!) else defaultMaterial
		}

		private fun getChatColorFromMap(map: Map<*, *>, key: String, defaultColor: ChatColor): ChatColor {
			val value = map[key]
			return if (value != null) ChatColor.valueOf((value as String?)!!) else defaultColor
		}

		private fun getStainedGlassPaneColor(map: Map<*, *>): Material {
			val color = map["color"].toString().uppercase(Locale.getDefault()) + "_STAINED_GLASS_PANE"
			return if (Material.matchMaterial(color) != null) Material.valueOf(color) else Material.GRAY_STAINED_GLASS_PANE
		}

		private fun setItemMeta(item: ItemStack, map: Map<*, *>, key: String) {
			val meta = item.itemMeta!!
			meta.lore = listOf(ChatColor.GRAY.toString() + map[key] as String?)
			item.setItemMeta(meta)
		}

		private val otherSettingsGUI: Inventory
			get() {
				val gui = Bukkit.createInventory(
					null, 45, ChatColor.YELLOW.toString() + "Bug Report - " + getValueFromLanguageFile(
						"buttonNames.otherSettings", "Other Settings"
					)
				)

				for (i in 0..8) {
					gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "))
				}

				for (i in 36..44) {
					gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "))
				}

				for (i in 9..35) {
					if (i % 9 == 0 || i % 9 == 8) {
						gui.setItem(i, createButton(Material.GRAY_STAINED_GLASS_PANE, " "))
					}
				}

				val enableTitleMessage: ItemStack = createButton(
					Material.PAPER, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
						"buttonNames.enableTitleMessage", "Enable Title Message"
					)
				)
				val enablePlayerHeads: ItemStack = createButton(
					Material.PLAYER_HEAD, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
						"buttonNames.enablePlayerHeads", "Enable Player Heads"
					)
				)
				val enableReportBook: ItemStack = createButton(
					Material.WRITABLE_BOOK, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
						"buttonNames.enableReportBook", "Enable Report Book"
					)
				)
				val onIcon: ItemStack = createButton(
					Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
				)
				val offIcon: ItemStack = createButton(
					Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
				)

				gui.setItem(10, enableTitleMessage)
				gui.setItem(11, enablePlayerHeads)
				gui.setItem(12, enableReportBook)

				gui.setItem(19, if (titleMessage) onIcon else offIcon)
				gui.setItem(20, if (playerHead) onIcon else offIcon)
				gui.setItem(21, if (reportBook) onIcon else offIcon)

				gui.setItem(
					40, createButton(
						Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile(
							"buttonNames.back", "Back"
						)
					)
				)

				return gui
			}

		private val titleMessage: Boolean
			get() = config.getBoolean("useTitleInsteadOfMessage")

		private fun setTitleMessage(player: Player) {
			playButtonClickSound(player)

			val toggle = titleMessage
			config.set("useTitleInsteadOfMessage", !toggle)
			saveConfig()
			if (debugMode) {
				plugin.logger.info("Title message set to " + !toggle)
			}
			player.openInventory.setItem(
				19, if (titleMessage) createButton(
					Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
				) else createButton(
					Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
				)
			)
		}

		private val playerHead: Boolean
			get() = config.getBoolean("enablePlayerHeads")

		private fun setPlayerHead(player: Player) {
			playButtonClickSound(player)

			val toggle = playerHead
			config.set("enablePlayerHeads", !toggle)
			saveConfig()
			if (debugMode) {
				plugin.logger.info("Player heads set to " + !toggle)
			}
			player.openInventory.setItem(
				20, if (playerHead) createButton(
					Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
				) else createButton(
					Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
				)
			)
		}

		private val reportBook: Boolean
			get() = config.getBoolean("enablePluginReportBook")

		private fun setReportBook(player: Player) {
			playButtonClickSound(player)

			val toggle = reportBook
			config.set("enablePluginReportBook", !toggle)
			saveConfig()
			if (debugMode) {
				plugin.logger.info("Report book set to " + !toggle)
			}
			player.openInventory.setItem(
				21, if (reportBook) createButton(
					Material.LIME_DYE, ChatColor.GREEN.toString() + getValueFromLanguageFile("buttonNames.true", "On")
				) else createButton(
					Material.GRAY_DYE, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.false", "Off")
				)
			)
		}

		private fun handleCancel(player: Player, clickMap: MutableMap<UUID, String?>) {
			sendMessageOrTitle(player, "cancelled", HashMap())
			clickMap.remove(player.uniqueId)
		}

		private fun sendMessageOrTitle(player: Player, key: String, replacements: Map<String, String?>) {
			var message: String = getValueFromLanguageFile(key, key)

			if (replacements.isNotEmpty()) {
				for ((key1, value) in replacements) {
					message = message.replace(key1, value!!)
				}
			}

			if (checkForKey("useTitleInsteadOfMessage", true)) {
				player.sendTitle(returnStartingMessage(ChatColor.GREEN), message, 10, 70, 20)
			} else {
				player.sendMessage(returnStartingMessage(ChatColor.GREEN) + message)
			}
		}

		private val typedStatusList: MutableList<MutableMap<String, Any>>
			get() {
				val originalStatuses: List<Map<*, *>> = config.getMapList("statuses")
				val statuses: MutableList<MutableMap<String, Any>> = ArrayList()
				for (originalMap in originalStatuses) {
					val typedMap: MutableMap<String, Any> = HashMap()
					for ((key, value) in originalMap) {
						if (key is String) {
							typedMap[key] = value!!
						}
					}
					statuses.add(typedMap)
				}
				return statuses
			}

		private fun updateStatus(
			statuses: List<MutableMap<String, Any>>,
			player: Player,
			clickMap: Map<UUID, String?>,
			updateKey: String,
			newValue: Any,
		) {
			statuses.forEach(Consumer<MutableMap<String, Any>> { statusMap: MutableMap<String, Any> ->
				if (statusMap["name"] == clickMap[player.uniqueId] && statusMap["id"] == savedStatusID) {
					statusMap[updateKey] = newValue
					config.set("statuses", statuses)
					saveConfig()
				}
			})
		}

		@EventHandler(priority = EventPriority.NORMAL)
		fun onPlayerChat(event: AsyncPlayerChatEvent) {
			val player = event.player

			if (setMaxReportsClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, setMaxReportsClickMap, getValueFromLanguageFile(
						"buttonNames.setMaxReportsPerPlayer", "Set Max Reports Per Player"
					)
				) { value: String ->
					var maxReportsValues = value
					val maxReports: Int
					if (maxReportsValues.matches("[0-9]+".toRegex())) {
						try {
							maxReports = maxReportsValues.toInt()
						} catch (e: NumberFormatException) {
							if (checkForKey("useTitleInsteadOfMessage", true)) {
								player.sendTitle(
									returnStartingMessage(ChatColor.RED), getValueFromLanguageFile(
										"enterValidNumber", "Please enter a valid number"
									), 10, 70, 20
								)
							} else {
								player.sendMessage(
									returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
										"enterValidNumber", "Please enter a valid number"
									)
								)
							}
							return@handleSettingUpdate
						}
						config.set("max-reports-per-player", maxReports)
						saveConfig()

						if (checkForKey("useTitleInsteadOfMessage", true)) {
							player.sendTitle(
								returnStartingMessage(ChatColor.GREEN), getValueFromLanguageFile(
									"maxReportsPerPlayerSuccessMessage",
									"Max reports per player has been set to %amount%"
								).replace("%amount%", maxReports.toString()), 10, 70, 20
							)
						} else {
							player.sendMessage(
								returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
									"maxReportsPerPlayerSuccessMessage",
									"Max reports per player has been set to %amount%"
								).replace("%amount%", maxReports.toString())
							)
						}
					} else {
						maxReportsValues = maxReportsValues.substring(0, 1).uppercase(Locale.getDefault()) + maxReportsValues.substring(1)
							.lowercase(Locale.getDefault())
						val customDisplayName: String = getEnglishValueFromValue(maxReportsValues).toString()
						if (customDisplayName.contains("Cancel")) {
							player.sendMessage(
								returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
									"buttonNames.cancelled", "Cancelled"
								)
							)
						} else {
							if (checkForKey("useTitleInsteadOfMessage", true)) {
								player.sendTitle(
									returnStartingMessage(ChatColor.RED), getValueFromLanguageFile(
										"enterValidNumber", "Please enter a valid number"
									), 10, 70, 20
								)
							} else {
								player.sendMessage(
									returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
										"enterValidNumber", "Please enter a valid number"
									)
								)
							}
						}
					}
				}
			}

			if (setReportCooldownClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, setReportCooldownClickMap, "Set Report Cooldown"
				) { value: String ->
					val reportCooldown: Int
					try {
						reportCooldown = value.toInt()
					} catch (e: NumberFormatException) {
						if (checkForKey("useTitleInsteadOfMessage", true)) {
							player.sendTitle(
								returnStartingMessage(ChatColor.RED), getValueFromLanguageFile(
									"enterValidNumber", "Please enter a valid number"
								), 10, 70, 20
							)
						} else {
							player.sendMessage(
								returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
									"enterValidNumber", "Please enter a valid number"
								)
							)
						}
						return@handleSettingUpdate
					}

					config.set("report-cooldown", reportCooldown)
					saveConfig()
					if (checkForKey("useTitleInsteadOfMessage", true)) {
						player.sendTitle(
							returnStartingMessage(ChatColor.GREEN), getValueFromLanguageFile(
								"reportCooldownSuccessMessage", "Report cooldown has been set to %time% seconds"
							).replace("%time%", reportCooldown.toString()), 10, 70, 20
						)
					} else {
						player.sendMessage(
							returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
								"reportCooldownSuccessMessage", "Report cooldown has been set to %time% seconds"
							).replace("%time%", reportCooldown.toString())
						)
					}
				}
			}

			if (removeStatusClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, removeStatusClickMap, savedStatusName
				) { value: String ->
					if (event.message.equals("cancel", ignoreCase = true)) {
						handleCancel(player, removeStatusClickMap)
						return@handleSettingUpdate
					}
					if (value.equals("confirm", ignoreCase = true)) {
						val statuses = typedStatusList
						statuses.removeIf { statusMap: Map<String, Any> -> statusMap["name"] == removeStatusClickMap[player.uniqueId] && statusMap["id"] == savedStatusID }
						config.set("statuses", statuses)
						saveConfig()

						val replacements: MutableMap<String, String?> = HashMap()
						replacements["%status%"] = savedStatusName
						sendMessageOrTitle(player, "statusRemoved", replacements)
					}
				}
			}

			if (setNewNameClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, setNewNameClickMap, "savedStatusName"
				) { value: String? ->
					if (event.message.equals("cancel", ignoreCase = true)) {
						handleCancel(player, setNewNameClickMap)
						return@handleSettingUpdate
					}
					config.set("name", value)
					saveConfig()

					val replacements: MutableMap<String, String?> = HashMap()
					replacements["%name%"] = value
					sendMessageOrTitle(player, "statusRenamed", replacements)
				}
			}

			if (renameStatusClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, renameStatusClickMap, savedStatusName
				) { value: String ->
					if (event.message.equals("cancel", ignoreCase = true)) {
						handleCancel(player, renameStatusClickMap)
						return@handleSettingUpdate
					}
					val statuses: List<MutableMap<String, Any>> = typedStatusList
					updateStatus(statuses, player, renameStatusClickMap, "name", value)

					val replacements: MutableMap<String, String?> = HashMap()
					replacements["%status%"] = value
					sendMessageOrTitle(player, "statusRenamed", replacements)
				}
			}

			if (setNewMaterialStatusClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, setNewMaterialStatusClickMap, savedStatusName
				) { value: String ->
					if (event.message.equals("cancel", ignoreCase = true)) {
						handleCancel(player, setNewMaterialStatusClickMap)
						return@handleSettingUpdate
					}
					val statuses: List<MutableMap<String, Any>> = typedStatusList
					updateStatus(statuses, player, setNewMaterialStatusClickMap, "icon", value)

					val replacements: MutableMap<String, String?> = HashMap()
					replacements["%material%"] = value
					sendMessageOrTitle(player, "statusMaterialChanged", replacements)
				}
			}

			if (setNewColorStatusClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, setNewColorStatusClickMap, savedStatusName
				) { value: String ->
					if (event.message.equals("cancel", ignoreCase = true)) {
						handleCancel(player, setNewColorStatusClickMap)
						return@handleSettingUpdate
					}
					if (!BugReportCommand.checkIfChatColorIsValid(value)) {
						if (checkForKey("useTitleInsteadOfMessage", true)) {
							player.sendTitle(
								returnStartingMessage(ChatColor.RED), getValueFromLanguageFile(
									"invalidColor", "Invalid color. Please enter a valid color"
								), 10, 70, 20
							)
						} else {
							player.sendMessage(
								returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
									"invalidColor", "Invalid color. Please enter a valid color"
								)
							)
						}
						return@handleSettingUpdate
					}

					val statuses: List<MutableMap<String, Any>> = typedStatusList
					updateStatus(statuses, player, setNewColorStatusClickMap, "color", value)

					val replacements: MutableMap<String, String?> = HashMap()
					replacements["%color%"] = value
					sendMessageOrTitle(player, "statusColorChanged", replacements)
				}
			}

			if (setNewDescriptionStatusClickMap.containsKey(player.uniqueId)) {
				handleSettingUpdate(
					event, player, setNewDescriptionStatusClickMap, savedStatusName
				) { value: String ->
					if (event.message.equals("cancel", ignoreCase = true)) {
						handleCancel(player, setNewDescriptionStatusClickMap)
						return@handleSettingUpdate
					}
					val statuses: List<MutableMap<String, Any>> = typedStatusList
					updateStatus(statuses, player, setNewDescriptionStatusClickMap, "description", value)

					val replacements: MutableMap<String, String?> = HashMap()
					replacements["%description%"] = value
					sendMessageOrTitle(player, "statusDescriptionChanged", replacements)
				}
			}
		}

		private fun handleSettingUpdate(
			event: AsyncPlayerChatEvent,
			player: Player,
			settingClickMap: MutableMap<UUID, String?>,
			displayName: String?,
			updateLogic: Consumer<String>,
		) {
			val clickDisplayName = settingClickMap[player.uniqueId]
			val displayNameDefault: String = getEnglishValueFromValue(displayName).toString()

			if (clickDisplayName != null && clickDisplayName == displayNameDefault) {
				event.isCancelled = true

				val scheduler = Bukkit.getServer().scheduler
				scheduler.runTask(plugin, Runnable {
					updateLogic.accept(event.message)
					player.openInventory(getSettingsGUI())
					settingClickMap.remove(player.uniqueId)
				})
			}
		}

		companion object {
			private fun setLanguage(languageCode: String, languageName: String, player: Player) {
				player.closeInventory()
				if (checkForKey("useTitleInsteadOfMessage", true)) {
					player.sendTitle(
						returnStartingMessage(ChatColor.GREEN),
						getValueFromLanguageFile("languageSetTo", "Language set to %language%").replace(
							"%language%", languageName
						),
						10,
						70,
						20
					)
				} else {
					player.sendMessage(
						returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
							"languageSetTo", "Language set to %language%"
						).replace("%language%", languageName)
					)
				}

				bugreportGUI.updateBugReportItems()
				config.set("language", languageCode)
				setPluginLanguage(languageCode)

				if (debugMode) {
					plugin.logger.info("Language set to $languageCode")
				}
				reloadConfig()

				player.openInventory(openLanguageGUI())
			}
		}
	}
}
