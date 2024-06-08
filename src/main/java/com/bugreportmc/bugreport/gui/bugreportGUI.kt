package com.bugreportmc.bugreport.gui

import com.bugreportmc.bugreport.BugReportLanguage.Companion.getValueFromLanguageFile
import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportManager.Companion.createButton
import com.bugreportmc.bugreport.BugReportManager.Companion.createEmptyItem
import com.bugreportmc.bugreport.BugReportManager.Companion.createInfoItem
import com.bugreportmc.bugreport.BugReportManager.Companion.getReportByKey
import com.bugreportmc.bugreport.BugReportManager.Companion.returnStartingMessage
import com.bugreportmc.bugreport.BugReportManager.Companion.translateTimestampToDate
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.BugReportSettings
import com.bugreportmc.bugreport.api.DataSource
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.keys.guiTextures
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.Serial
import java.util.*

object bugreportGUI {
	private val bugReportItems: HashMap<String?, String?> = object : HashMap<String?, String?>() {
		@Serial
		private val serialVersionUID = 2870322851221649721L

		init {
			put("BugReporter", getValueFromLanguageFile("buttonNames.bugReportDetailsUsername", "Username"))
			put("BugReportUUID", getValueFromLanguageFile("buttonNames.bugReportDetailsUUID", "UUID"))
			put("BugReportWorld", getValueFromLanguageFile("buttonNames.bugReportDetailsWorld", "World"))
			put("BugReportMessage", getValueFromLanguageFile("buttonNames.bugReportDetailsFullMessage", "Full Message"))
			put("BugReportCategory", getValueFromLanguageFile("buttonNames.bugReportDetailsCategory", "Category ID"))
			put(
				"BugReportStatus",
				getValueFromLanguageFile("buttonNames.bugReportDetailsStatus", "Status") + " (Click to change)"
			)
			put("BugReportTimestamp", getValueFromLanguageFile("buttonNames.bugReportDetailsTimestamp", "Timestamp"))
			put(
				"BugReportLocation", (getValueFromLanguageFile(
					"buttonNames.bugReportDetailsLocation", "Location"
				) + ChatColor.BOLD).toString() + " (Click to teleport)"
			)
			put("BugReportGamemode", getValueFromLanguageFile("buttonNames.bugReportDetailsGamemode", "Gamemode"))
			put(
				"BugReportServerName", getValueFromLanguageFile("buttonNames.bugReportDetailsServerName", "Server Name")
			)
			put("BugReportUnArchive", getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"))
			put("BugReportArchive", getValueFromLanguageFile("buttonNames.archive", "Archive"))
			put("BugReportBack", getValueFromLanguageFile("buttonNames.back", "Back"))
			put("BugReportDelete", getValueFromLanguageFile("buttonNames.delete", "Delete"))
		}
	}

	fun generateNewYML() {
		val configFile = File(plugin.dataFolder, "custom_bug_report_details_GUI.yml")
		if (!configFile.exists()) {
			plugin.saveResource("custom_bug_report_details_GUI.yml", false)
		}
	}

	fun updateBugReportItems() {
		bugReportItems["BugReportUnArchive"] = getValueFromLanguageFile(
			key = "buttonNames.unarchive", defaultValue = "Unarchive"
		)
		bugReportItems["BugReportArchive"] = getValueFromLanguageFile(
			key = "buttonNames.archive", defaultValue = "Archive"
		)
		bugReportItems["BugReportBack"] = getValueFromLanguageFile(
			key = "buttonNames.back", defaultValue = "Back"
		)
		bugReportItems["BugReportDelete"] = getValueFromLanguageFile(
			key = "buttonNames.delete", defaultValue = "Delete"
		)
	}

	private fun loadGUIConfig(): YamlConfiguration? {
		val configFile = File(
			plugin.dataFolder, "custom_bug_report_details_GUI.yml"
		)

		if (!configFile.exists()) {
			plugin.logger.info("custom_bug_report_details_GUI.yml not found. Falling back to default GUI settings.")
			return null
		}

		return YamlConfiguration.loadConfiguration(configFile)
	}

	fun openBugReportDetailsGUI(player: Player, report: String?, reportIDGUI: Int, isArchivedGUI: Boolean) {
		updateBugReportItems()

		val guiConfig = loadGUIConfig()
		val bugReportTitle = if (isArchivedGUI) "Archived Bug Details - #" else "Bug Report Details - #"
		var guiSize: Int

		if (guiConfig != null && validateGUIConfig(guiConfig)) {
			guiSize = guiConfig.getInt("bug_report_details_GUI.guiSize")
			if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
				guiSize = 45
				Bukkit.getLogger().warning("Invalid GUI size in customGUI.yml. Falling back to default size.")
			}
		} else {
			guiSize = 45
		}

		val gui = Bukkit.createInventory(player, guiSize, ChatColor.YELLOW.toString() + bugReportTitle + reportIDGUI)

		if (guiConfig == null || !validateGUIConfig(guiConfig)) {
			setupDefaultGUI(gui, player, report, reportIDGUI, isArchivedGUI)
		} else {
			setupGUIFromConfig(gui, player, guiConfig, report!!, reportIDGUI, isArchivedGUI)
		}

		player.openInventory(gui)
		getPluginManager().registerEvents(
			BugReportManager.BugReportDetailsListener(gui, reportIDGUI), plugin
		)
	}

	private fun setupGUIFromConfig(
		gui: Inventory,
		player: Player,
		guiConfig: YamlConfiguration,
		report: String,
		reportIDGUI: Int,
		isArchivedGUI: Boolean,
	) {
		if (!validateGUIConfig(guiConfig)) {
			plugin.logger.severe("The layout of the customGUI.yml file is incorrect. Falling back to the default layout.")
			logErrorMessage("The layout of the customGUI.yml file is incorrect. Falling back to the default layout")
			setupDefaultGUI(gui, player, report, reportIDGUI, isArchivedGUI)
			return
		}

		val itemsList = guiConfig.getList("bug_report_details_GUI.items")!!
		for (itemObj in itemsList) {
			if (itemObj is Map<*, *>) {
				try {
					val slot: Int = itemObj["slot"].toString().toInt()
					val bugReportItem: String = itemObj["bugReportItem"].toString()
					val materialKey: String = itemObj["material"].toString()
					var material: Material
					var itemStack: ItemStack?

					if ((isArchivedGUI && bugReportItem == "BugReportArchive") || (!isArchivedGUI && bugReportItem == "BugReportUnArchive")) {
						continue
					}

					if (materialKey.contains("[") && materialKey.contains("]")) {
						val materials = materialKey.replace("[\\[\\]]".toRegex(), "").split(",\\s*".toRegex())
							.dropLastWhile { it.isEmpty() }.toTypedArray()
						material =
							Material.valueOf(if (isArchivedGUI) materials[1].trim { it <= ' ' } else materials[0].trim { it <= ' ' })
					} else {
						material = Material.valueOf(materialKey)
					}

					val textureObj: Any? = itemObj["texture"]
					val texture = textureObj.toString()

					val reportDetails = parseReportDetails(report)
					if (bugReportItem == "BugReportMessage") {
						val messageItem =
							createItemForReportDetail(bugReportItem, material, texture, reportDetails, isArchivedGUI)
						val meta = messageItem.itemMeta!!
						meta.setDisplayName(meta.displayName)

						val fullMessage = reportDetails.getOrDefault("Full Message", "N/A")
						if (fullMessage.length > 32) {
							val lore: MutableList<String> = ArrayList()
							val words = fullMessage.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
							var currentLine = StringBuilder()
							for (word in words) {
								if (currentLine.length + word.length > 30) {
									lore.add(currentLine.toString())
									currentLine = StringBuilder()
								}

								currentLine.append(word).append(" ")
							}

							if (currentLine.isNotEmpty()) lore.add(currentLine.toString())
							meta.lore = lore
						} else {
							meta.lore = listOf(ChatColor.WHITE.toString() + fullMessage)
						}

						messageItem.setItemMeta(meta)
						itemStack = messageItem
						gui.setItem(slot, itemStack)
					} else {
						itemStack =
							createItemForReportDetail(bugReportItem, material, texture, reportDetails, isArchivedGUI)
						gui.setItem(slot, itemStack)
					}
				} catch (e: IllegalArgumentException) {
					Bukkit.getLogger().warning("Error parsing material or slot number: " + e.message)
				}
			}
		}
	}

	/**
	 * Validates the custom GUI configuration.
	 *
	 * @param guiConfig The custom GUI configuration.
	 * @return true if the configuration is valid; false otherwise.
	 */
	private fun validateGUIConfig(guiConfig: YamlConfiguration): Boolean {
		val guiSize = guiConfig.getInt("bug_report_details_GUI.guiSize", -1)
		if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
			Bukkit.getLogger()
				.warning("Invalid GUI size defined in customGUI.yml. Must be between 9 and 54 and a multiple of 9.")
			return false
		}

		val itemsList = guiConfig.getList("bug_report_details_GUI.items")
		if (itemsList == null || itemsList.isEmpty()) {
			Bukkit.getLogger().warning("No items defined in customGUI.yml or incorrect format.")
			return false
		}

		for (itemObj in itemsList) {
			if (itemObj !is Map<*, *>) {
				return false
			}

			if (!itemObj.containsKey("slot") || !itemObj.containsKey("bugReportItem") || !itemObj.containsKey("material")) {
				return false
			}

			try {
				itemObj["slot"].toString().toInt()
			} catch (e: NumberFormatException) {
				return false
			}

			if (itemObj["slot"].toString().toInt() >= guiSize) {
				Bukkit.getLogger().warning("Slot number in customGUI.yml is greater than the GUI size.")
				return false
			}

			if (!bugReportItems.containsKey(itemObj["bugReportItem"].toString())) {
				Bukkit.getLogger().warning("Invalid bugReportItem in customGUI.yml: " + itemObj["bugReportItem"])
				return false
			}

			if ("BugReporter" == itemObj["bugReportItem"].toString()) {
				if ("%player_texture%" != itemObj["texture"]) {
					Bukkit.getLogger()
						.warning("Texture for BugReporter item in customGUI.yml must be %player_texture%.")
					return false
				}
			}
		}

		return true
	}

	private fun createItemForReportDetail(
		bugReportItemKey: String,
		defaultMaterial: Material,
		textureBase64: String?,
		reportDetails: Map<String, String>,
		isArchivedGUI: Boolean,
	): ItemStack {
		val reportDetailKey = deriveReportDetailKey(bugReportItemKey)
		var detailValue: String? = reportDetails.getOrDefault(reportDetailKey, "N/A")
		val item: ItemStack

		when (bugReportItemKey) {
			"BugReportTimestamp" -> if (detailValue != null) {
				detailValue = translateTimestampToDate(detailValue.toLong())
			}

			"BugReportCategory" -> {
				val categoryID = reportDetails.getOrDefault("Category ID", "N/A")
				if ("N/A" != categoryID) {
					val categoryList: List<Map<*, *>> = BugReportManager.config.getMapList("reportCategories")
					detailValue = categoryList.stream()
						.filter { categoryMap: Map<*, *> -> categoryID == categoryMap["id"].toString() }
						.map<String> { categoryMap: Map<*, *> -> categoryMap["name"] as String? }.findFirst()
						.orElse("Unknown Category")
				}
			}

			"BugReporter" -> {
				val username = reportDetails["Username"]

				if (BugReportManager.config.getBoolean("enablePlayerHeads")) {
					item = DataSource.getPlayerHead(username)
				} else {
					item = createInfoItem(
						Material.PLAYER_HEAD,
						ChatColor.GOLD.toString() + "Username",
						ChatColor.WHITE.toString() + username,
						false
					)
				}
				val meta = item.itemMeta
				if (meta != null) {
					meta.setDisplayName(
						ChatColor.YELLOW.toString() + bugReportItems.getOrDefault(
							bugReportItemKey, "Unknown Item"
						)
					)
					val lore: MutableList<String> = ArrayList()
					lore.add(ChatColor.WHITE.toString() + username)
					meta.lore = lore
					item.setItemMeta(meta)
				}
				return item
			}

			"BugReportStatus" -> detailValue = if (isArchivedGUI) "Archived" else "Active"
			"BugReportBack", "BugReportDelete", "BugReportArchive", "BugReportUnArchive" -> detailValue = null
		}
		if (isItemSupportsTexture(bugReportItemKey) && textureBase64 != null && textureBase64.trim { it <= ' ' }
				.isNotEmpty()) {
			item = bugReportItems.getOrDefault(bugReportItemKey, "Unknown Item")?.let {
				BugReportSettings.createCustomPlayerHead(
					textureBase64, it, 1
				)
			}!!
		} else {
			var statusItem: ItemStack
			val status = reportDetails["Status"]

			if (status != null) {
				val statuses: List<Map<*, *>> = BugReportManager.config.getMapList("statuses")
				for (statusMap in statuses) {
					if (statusMap["id"].toString() == status) {
						val statusName = statusMap["name"].toString()
						val statusDescription = statusMap["description"].toString()

						val statusColor =
							ChatColor.valueOf(statusMap["color"].toString().uppercase(Locale.getDefault()))
						val statusIcon =
							if (Material.matchMaterial((statusMap["icon"])!!.toString()) != null) Material.matchMaterial(
								(statusMap["icon"] as String?)!!
							) else Material.BARRIER
						statusItem = statusIcon?.let {
							createInfoItem(
								it,
								"$statusColor$statusName (Click to change)",
								statusColor.toString() + statusDescription,
								false
							)
						}!!

						if (bugReportItemKey == "BugReportStatus") {
							item = statusItem
							return item
						}
					}
				}
			}

			item = ItemStack(defaultMaterial, 1)
			val meta = item.itemMeta

			if (meta != null) {
				meta.setDisplayName(
					ChatColor.YELLOW.toString() + bugReportItems.getOrDefault(
						bugReportItemKey, "Unknown Item"
					)
				)
				val lore: MutableList<String> = ArrayList()
				if (detailValue != null) {
					lore.add(ChatColor.WHITE.toString() + detailValue)
				}
				meta.lore = lore
				item.setItemMeta(meta)
			}
		}
		return item
	}

	/**
	 * Derives the report detail key from the bug report item key.
	 *
	 * @param bugReportItemKey The key of the bug report item.
	 * @return The derived report detail key.
	 */
	@Contract(pure = true)
	private fun deriveReportDetailKey(bugReportItemKey: String): String {
		return when (bugReportItemKey) {
			"BugReporter" -> getValueFromLanguageFile("buttonNames.bugReportDetailsUsername", "Username")
			"BugReportUUID" -> getValueFromLanguageFile("buttonNames.bugReportDetailsUUID", "UUID")
			"BugReportWorld" -> getValueFromLanguageFile("buttonNames.bugReportDetailsWorld", "World")
			"BugReportMessage" -> getValueFromLanguageFile("buttonNames.bugReportDetailsFullMessage", "Full Message")
			"BugReportCategory" -> getValueFromLanguageFile("buttonNames.bugReportDetailsCategory", "Category ID")
			"BugReportStatus" -> getValueFromLanguageFile(
				"buttonNames.bugReportDetailsStatus", "Status"
			) + " (Click to change)"

			"BugReportTimestamp" -> getValueFromLanguageFile("buttonNames.bugReportDetailsTimestamp", "Timestamp")
			"BugReportLocation" -> (getValueFromLanguageFile(
				"buttonNames.bugReportDetailsLocation", "Location"
			) + ChatColor.BOLD).toString() + " (Click to teleport)"

			"BugReportGamemode" -> getValueFromLanguageFile("buttonNames.bugReportDetailsGamemode", "Gamemode")
			"BugReportServerName" -> getValueFromLanguageFile("buttonNames.bugReportDetailsServerName", "Server Name")
			"BugReportUnArchive" -> getValueFromLanguageFile("buttonNames.unarchive", "Unarchive")
			"BugReportArchive" -> getValueFromLanguageFile("buttonNames.archive", "Archive")
			"BugReportBack" -> getValueFromLanguageFile("buttonNames.back", "Back")
			"BugReportDelete" -> getValueFromLanguageFile("buttonNames.delete", "Delete")
			else -> bugReportItemKey.replace("BugReport", "")
		}
	}

	/**
	 * Checks if the given item key supports custom textures.
	 *
	 * @param bugReportItemKey The key of the bug report item.
	 * @return true if the item supports custom textures; false otherwise.
	 */
	private fun isItemSupportsTexture(bugReportItemKey: String): Boolean {
		return listOf(
			"BugReportUUID",
			"BugReportWorld",
			"BugReportMessage",
			"BugReportCategory",
			"BugReportTimestamp",
			"BugReportLocation",
			"BugReportGamemode",
			"BugReportServerName",
			"BugReportUnArchive",
			"BugReportArchive",
			"BugReportDelete"
		).contains(bugReportItemKey)
	}

	private fun parseReportDetails(report: String): Map<String, String> {
		val details: MutableMap<String, String> = HashMap()
		val lines = report.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		for (line in lines) {
			val parts = line.split(":".toRegex(), limit = 2).toTypedArray()
			if (parts.size == 2) {
				val key = parts[0].trim { it <= ' ' }
				val value = parts[1].trim { it <= ' ' }
				details[key] = value
			}
		}
		return details
	}

	fun setupDefaultGUI(gui: Inventory, player: Player, report: String?, reportIDGUI: Int?, isArchivedGUI: Boolean) {
		if (report == null) {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + " Error 101: Report is null. Please report this to the plugin developer.")
			return
		}

		val username: String = getReportByKey(report, "Username").toString()
		val uuid: String = getReportByKey(report, "UUID").toString()
		val world: String = getReportByKey(report, "World").toString()
		val fullMessage: String = getReportByKey(report, "Full Message").toString()
		val category: String = getReportByKey(report, "Category ID").toString()
		var location: String = getReportByKey(report, "Location").toString()
		var gamemode: String = getReportByKey(report, "Gamemode").toString()
		var status: String = getReportByKey(report, "Status").toString()
		val serverName: String = getReportByKey(report, "serverName").toString()

		val emptyItem: ItemStack = createEmptyItem()
		val locationTitle: String

		if (location == "null") location = "Not found"

		if (location.length - location.replace(",", "").length != 3) {
			location = "Not found"
			locationTitle = "Location"
		} else {
			locationTitle = "Location " + ChatColor.BOLD + "(Click to teleport)"
		}

		if (gamemode == "null") gamemode = "Unknown"

		val usernameItem: ItemStack
		if (BugReportManager.config.getBoolean("enablePlayerHeads")) {
			usernameItem = DataSource.getPlayerHead(username)
		} else {
			usernameItem = createInfoItem(
				Material.PLAYER_HEAD,
				ChatColor.GOLD.toString() + "Username",
				ChatColor.WHITE.toString() + username,
				false
			)
		}

		val isLongMessage = fullMessage.length > 32

		val timestampToDate = getReportByKey(report, "Timestamp")?.let { translateTimestampToDate(it.toLong()) }
		val uuidItem: ItemStack = createInfoItem(
			Material.NAME_TAG, ChatColor.GOLD.toString() + "UUID", ChatColor.WHITE.toString() + uuid, false
		)
		val worldItem: ItemStack = createInfoItem(
			Material.GRASS_BLOCK, ChatColor.GOLD.toString() + "World", ChatColor.WHITE.toString() + world, false
		)
		val messageItem: ItemStack = createInfoItem(
			Material.PAPER,
			ChatColor.GOLD.toString() + "Full Message",
			ChatColor.WHITE.toString() + fullMessage,
			isLongMessage
		)
		val serverNameItem: ItemStack = createInfoItem(
			Material.COMPASS, ChatColor.GOLD.toString() + "Server Name", ChatColor.WHITE.toString() + serverName, false
		)
		var statusItem: ItemStack? = null

		val statuses: List<Map<*, *>> = BugReportManager.config.getMapList("statuses")
		var statusFound = false

		for (statusMap in statuses) {
			if (status == "0") {
				status = if (isArchivedGUI) "Archived" else "Active"
				val stautsMaterial = if (isArchivedGUI) Material.RED_DYE else Material.LIME_DYE
				val statusChatColor = if (isArchivedGUI) ChatColor.RED else ChatColor.GREEN

				statusItem = createInfoItem(
					stautsMaterial,
					statusChatColor.toString() + "Status (Click to change)",
					statusChatColor.toString() + status,
					false
				)
				statusFound = true
				break
			}

			if (statusMap["id"].toString() == status) {
				val statusName = statusMap["name"].toString()
				val statusColor = statusMap["color"].toString()
				val statusChatColor = ChatColor.valueOf(statusColor.uppercase(Locale.getDefault()))
				var statusMaterial = statusMap["icon"].toString().uppercase(Locale.getDefault())

				if (statusMaterial.contains("MINECRAFT:")) {
					statusMaterial = statusMaterial.replace("MINECRAFT:", "")
				}

				statusItem = createInfoItem(
					Material.valueOf(statusMaterial),
					statusChatColor.toString() + "Status (Click to change)",
					statusChatColor.toString() + statusName,
					false
				)
				statusFound = true
				break
			}
		}

		if (!statusFound) {
			statusItem = createInfoItem(
				Material.BARRIER,
				ChatColor.GOLD.toString() + "Status (Click to change)",
				ChatColor.GOLD.toString() + "Unknown",
				false
			)
		}

		val timestampItem: ItemStack = createInfoItem(
			Material.CLOCK, ChatColor.GOLD.toString() + "Timestamp", ChatColor.WHITE.toString() + timestampToDate, false
		)
		val locationItem: ItemStack = createInfoItem(
			Material.COMPASS, ChatColor.GOLD.toString() + locationTitle, ChatColor.WHITE.toString() + location, false
		)
		val gamemodeItem: ItemStack = createInfoItem(
			Material.DIAMOND_SWORD, ChatColor.GOLD.toString() + "Gamemode", ChatColor.WHITE.toString() + gamemode, false
		)
		val backButton: ItemStack = createButton(
			Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.back", "Back")
		)
		val archiveButton = BugReportSettings.createCustomPlayerHead(
			guiTextures.archiveTexture,
			ChatColor.YELLOW.toString() + getValueFromLanguageFile("buttonNames.archive", "Archive"),
			16
		)
		val unarchiveButton = BugReportSettings.createCustomPlayerHead(
			guiTextures.unarchiveTexture,
			ChatColor.YELLOW.toString() + getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"),
			17
		)
		val deleteButton = BugReportSettings.createCustomPlayerHead(
			guiTextures.deleteTexture,
			ChatColor.YELLOW.toString() + getValueFromLanguageFile("buttonNames.delete", "Delete"),
			18
		)

		for (i in 0 until gui.size) {
			gui.setItem(i, emptyItem)
		}

		gui.setItem(0, usernameItem)
		gui.setItem(2, uuidItem)
		gui.setItem(4, worldItem)
		gui.setItem(6, messageItem)
		gui.setItem(8, serverNameItem)

		gui.setItem(20, statusItem)
		gui.setItem(22, timestampItem)
		gui.setItem(24, locationItem)
		gui.setItem(26, gamemodeItem)

		gui.setItem(38, if (!isArchivedGUI) archiveButton else unarchiveButton)
		gui.setItem(40, backButton)
		gui.setItem(42, deleteButton)

		if ("null" != category && "" != category) {
			val categoryList: List<Map<*, *>> = BugReportManager.config.getMapList("reportCategories")

			val categoryNameOptional = categoryList.stream()
				.filter { categoryMap: Map<*, *> -> categoryMap["id"].toString().toInt() == category.toInt() }
				.map { categoryMap: Map<*, *> -> categoryMap["name"].toString() }.findFirst()

			if (categoryNameOptional.isPresent) {
				val categoryName = categoryNameOptional.get()
				val categoryItem: ItemStack = createInfoItem(
					Material.CHEST,
					ChatColor.GOLD.toString() + "Category Name",
					ChatColor.WHITE.toString() + categoryName,
					false
				)
				gui.setItem(18, categoryItem)
			}
		} else {
			val categoryItem: ItemStack = createInfoItem(
				Material.CHEST, ChatColor.GOLD.toString() + "Category Name", ChatColor.WHITE.toString() + "None", false
			)
			gui.setItem(18, categoryItem)
		}

		player.openInventory(gui)
		Bukkit.getPluginManager()
			.registerEvents(BugReportManager.BugReportDetailsListener(gui, reportIDGUI!!), plugin)
	}
}
