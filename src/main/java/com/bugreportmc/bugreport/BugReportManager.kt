package com.bugreportmc.bugreport

import com.bugreportmc.bugreport.BugReportDatabase.Companion.getBugReportLocation
import com.bugreportmc.bugreport.BugReportDatabase.Companion.getStaticUUID
import com.bugreportmc.bugreport.BugReportDatabase.Companion.updateBugReportArchive
import com.bugreportmc.bugreport.BugReportLanguage.Companion.getEnglishValueFromValue
import com.bugreportmc.bugreport.BugReportLanguage.Companion.getValueFromLanguageFile
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.BugReportSettings.getSettingsGUI
import com.bugreportmc.bugreport.BugReportSettings.getStatusSelectionGUI
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.commands.BugReportCommand.Companion.getChatColorByCode
import com.bugreportmc.bugreport.commands.BugReportCommand.Companion.stringColorToColorCode
import com.bugreportmc.bugreport.discord.LinkDiscord
import com.bugreportmc.bugreport.extensions.PlanHook
import com.bugreportmc.bugreport.gui.BugReportConfirmationGUI
import com.bugreportmc.bugreport.gui.bugreportGUI
import com.bugreportmc.bugreport.listeners.PluginMessageListener
import com.bugreportmc.bugreport.listeners.ReportCreatedEvent
import org.bukkit.Bukkit.*
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.Serial
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class BugReportManager(private val plugin: Plugin) : Listener {
	var reportCategories: List<Category>? = null
	private val discord: LinkDiscord

	init {
		loadConfig()
		reportCategories = loadReportCategories()
		bugReports = HashMap()
		database = BugReportDatabase()

		loadBugReports()
		checkConfig()

		val webhookURL: String = config.getString("webhookURL", "").toString()
		pluginTitle = config.getString("pluginTitle", "[Bug Report]").toString()

		if (pluginTitle.contains("&")) {
			val parts = pluginTitle.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			for (i in 1 until parts.size) {
				val colorCode = extractValidColorCode(parts[i])

				if (colorCode != null) {
					val endingPluginTitleColorOther: ChatColor = getChatColorByCode("§$colorCode")
					endingPluginTitleColor = endingPluginTitleColorOther
				}
			}

			pluginTitle = pluginTitle.replace("&", "§")
		}

		pluginColor = stringColorToColorCode(
			Objects.requireNonNull<String>(
				config.getString(
					"pluginColor", "Yellow"
				)?.uppercase(Locale.getDefault())
			)
		)
		discord = LinkDiscord(webhookURL)
		reportCategories = loadReportCategories()
	}

	private fun loadReportCategories(): List<Category>? {
		if (checkCategoryConfig()) {
			val categories: MutableList<Category> = ArrayList()
			val categoryList: List<Map<*, *>> = config.getMapList("reportCategories")

			for (categoryMap in categoryList) {
				val name = categoryMap["name"].toString()
				val description = categoryMap["description"].toString()
				val itemString = categoryMap["item"].toString()
				val color = categoryMap["color"].toString().uppercase(Locale.getDefault())
				val id = categoryMap["id"].toString().toInt()

				val itemMaterial: Material = Material.matchMaterial(itemString) ?: continue

				val itemStack = ItemStack(itemMaterial)
				val itemMeta: ItemMeta = itemStack.itemMeta!!
				Objects.requireNonNull(itemMeta).setDisplayName(ChatColor.YELLOW.toString() + name)
				itemMeta.lore = listOf(ChatColor.GRAY.toString() + description)
				itemStack.setItemMeta(itemMeta)
				categories.add(Category(id, name, color, itemStack))
			}

			return categories
		} else {
			plugin.logger.warning(
				getValueFromLanguageFile(
					"wentWrongLoadingCategoriesMessage", "Something went wrong while loading the report categories"
				)
			)
			logErrorMessage(
				getValueFromLanguageFile(
					"wentWrongLoadingCategoriesMessage", "Something went wrong while loading the report categories"
				)
			)
			return null
		}
	}

	fun setWebhookURL(webhookURL: String) {
		if (debugMode) {
			plugin.logger.info("Setting Discord Webhook URL to $webhookURL")
		}
		config.set("webhookURL", webhookURL)
		saveConfig()
		discord.setWebhookURL(webhookURL)
	}

	fun submitBugReport(player: Player, message: String, categoryId: Int) {
		if (debugMode) {
			plugin.logger.info("Submitting bug report for " + player.name + "...")
		}
		val reports: MutableList<String> =
			bugReports.getOrDefault(getStaticUUID(), ArrayList<String>(listOf("DUMMY")))
				.toMutableList()
		val playerId: UUID = player.uniqueId

		val playerName: String = player.name
		val playerUUID: String = playerId.toString()
		val worldName: String = player.world.name
		val gamemode: String = player.gameMode.toString()
		val serverName: String = config.getString("serverName", "Unknown Server").toString()
		val location: String =
			player.world.name + ", " + player.location.blockX + ", " + player.location.blockY + ", " + player.location.blockZ

		val reportID = reports.stream().filter { report: String? -> report!!.contains("Report ID: ") }
			.reduce { _: String?, _: String? -> null }.map { report: String? ->
				Arrays.stream(
					report!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				).filter { line: String -> line.contains("Report ID:") }.findFirst().orElse("Report ID: 0")
			}.map { reportIDLine: String ->
				reportIDLine.split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].trim { it <= ' ' }
			}.orElse("0")

		val reportIDInt = (reportID.toInt() + 1).toString()
		val header = """
		 	Username: $playerName
		 	UUID: $playerUUID
		 	World: $worldName
		 	hasBeenRead: 0
		 	Category ID: $categoryId
		 	Full Message: $message
		 	Archived: 0
		 	Report ID: $reportIDInt
		 	Timestamp: ${System.currentTimeMillis()}
		 	Location: $location
		 	Gamemode: $gamemode
		 	""".trimIndent()

		reports += header
		bugReports = bugReports.toMutableMap().apply { put(playerId, ArrayList(reports)) }

		if (getPluginManager().isPluginEnabled("Plan")) {
			if (debugMode) {
				plugin.logger.info("Updating Plan hook for $playerName...")
			}
			PlanHook.instance?.updateHook(playerId, playerName)
		}

		if (debugMode) {
			plugin.logger.info("Adding bug report to database...")
		}
		database.addBugReport(playerName, playerId, worldName, header, message, location, gamemode, serverName)

		if (config.getBoolean("enableBugReportNotifications", true)) {
			if (debugMode) {
				plugin.logger.info("Sending bug report notification to online players...")
			}
			val defaultMessage = returnStartingMessage(ChatColor.GRAY) + getValueFromLanguageFile(
				"bugReportNotificationMessage", "A new bug report has been submitted by %player%!"
			).replace("%player%", ChatColor.AQUA.toString() + playerName + ChatColor.GRAY)

			for (onlinePlayer in getOnlinePlayers()) {
				if (onlinePlayer.hasPermission("bugreport.notify")) {
					onlinePlayer.sendMessage(defaultMessage)
				}
			}
		}

		if (config.getBoolean("enableDiscordWebhook", true)) {
			if (debugMode) {
				plugin.logger.info("Sending bug report to Discord...")
			}
			val webhookURL: String = config.getString("webhookURL", "").toString()
			if (webhookURL.isEmpty()) {
				plugin.logger.warning(
					getValueFromLanguageFile(
						"missingDiscordWebhookURLMessage", "Missing webhookURL in config.yml"
					)
				)
				logErrorMessage(
					getValueFromLanguageFile(
						"missingDiscordWebhookURLMessage", "Missing webhookURL in config.yml"
					)
				)
			}

			try {
				discord.sendBugReport(message, worldName, playerName, location, gamemode, categoryId, serverName)
				if (debugMode) {
					plugin.logger.info("Bug report sent to Discord.")
				}
			} catch (e: Exception) {
				plugin.logger.warning("Error sending bug report to Discord: " + e.message)
				logErrorMessage("Error sending bug report to Discord: " + e.message)
			}
		}

		if (getServer().messenger.isIncomingChannelRegistered(BugReportPlugin.plugin, "BungeeCord")) {
			PluginMessageListener.sendPluginMessage(player)
		}

		getScheduler().runTask(plugin, Runnable {
			val reportEvent = ReportCreatedEvent(header)
			getServer().pluginManager.callEvent(reportEvent)
		})
	}

	class BugReportListener : Listener {
		private val closingInventoryMap: MutableMap<UUID, Boolean> = HashMap()

		@EventHandler(priority = EventPriority.NORMAL)
		fun onInventoryClick(event: InventoryClickEvent) {
			val titleText = ChatColor.stripColor(event.view.title)

			if (debugMode) {
				plugin.logger.info("Clicked inventory: $titleText")
			}

			val isArchivedGUI = titleText!!.startsWith("Archived Bugs")

			if (!titleText.startsWith("Bug Report") && !isArchivedGUI) {
				return
			}

			event.isCancelled = true

			val player: Player = event.whoClicked as Player

			val clickedItem: ItemStack = event.currentItem!!
			if (clickedItem.type == Material.AIR) {
				return
			}

			val itemMeta: ItemMeta = clickedItem.itemMeta!!
			if (!itemMeta.hasDisplayName()) {
				return
			}

			val displayName: String = itemMeta.displayName
			val cleanedDisplayName = ChatColor.stripColor(displayName)
			val customDisplayName: String = getEnglishValueFromValue(displayName).toString()

			if (debugMode) {
				plugin.logger.info("Clicked item: $customDisplayName")
			}

			if (cleanedDisplayName!!.startsWith("Bug Report #")) {
				val reportID = displayName.substring(14).toInt()
				val reports: List<String?> = bugReports.getOrDefault(
					getStaticUUID(), ArrayList<String?>(
						listOf("DUMMY")
					)
				)
				val report =
					reports.stream().filter { reportString: String? -> reportString!!.contains("Report ID: $reportID") }
						.findFirst().orElse(null)

				if (debugMode) {
					plugin.logger.info("Opening bug report details GUI for report ID $reportID")
				}

				playButtonClickSound(player)
				bugreportGUI.openBugReportDetailsGUI(player, report, reportID, isArchivedGUI)
				return
			}

			when (Objects.requireNonNull<String>(customDisplayName)) {
				"Back" -> {
					val currentPage = getCurrentPage(player)
					if (currentPage > 1) {
						if (titleText.startsWith("Bug Report Details - ")) {
							playButtonClickSound(player)
							player.openInventory(
								if (isArchivedGUI) getArchivedBugReportsGUI(
									currentPage, player
								) else getBugReportGUI(currentPage, player)
							)
						} else {
							setCurrentPage(player, currentPage - 1)
							playButtonClickSound(player)
							localCurrentPage = currentPage - 1
							player.openInventory(
								if (isArchivedGUI) getArchivedBugReportsGUI(
									localCurrentPage, player
								) else getBugReportGUI(
									localCurrentPage, player
								)
							)
						}
					}
				}

				"Forward" -> {
					val currentPage = getCurrentPage(player)
					if (currentPage < totalPages) {
						setCurrentPage(player, currentPage + 1)
						playButtonClickSound(player)
						localCurrentPage = currentPage + 1
						player.openInventory(
							if (isArchivedGUI) getArchivedBugReportsGUI(localCurrentPage, player) else getBugReportGUI(
								localCurrentPage, player
							)
						)
					}
				}

				"Settings" -> {
					player.openInventory(getSettingsGUI())
					playButtonClickSound(player)
				}

				"Close" -> {
					closingInventoryMap[player.uniqueId] = true
					playButtonClickSound(player)
					player.closeInventory()
				}
			}
		}

		@EventHandler(priority = EventPriority.NORMAL)
		fun onInventoryClose(event: InventoryCloseEvent) {
			if (event.view.title.startsWith(ChatColor.YELLOW.toString() + "Bug Report")) {
				val player: Player = event.player as Player
				val playerId: UUID = player.uniqueId

				if (closingInventoryMap.getOrDefault(playerId, false)) {
					closingInventoryMap[playerId] = false
					return
				}

				closingInventoryMap.remove(playerId)
			}
		}
	}

	class BugReportDetailsListener(gui: Inventory, private val reportIDGUI: Int) : Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		fun onInventoryClick(event: InventoryClickEvent) {
			val title: String = event.view.title
			val isArchivedDetails = title.startsWith(ChatColor.YELLOW.toString() + "Archived Bug Details")

			if (!title.startsWith(ChatColor.YELLOW.toString() + "Bug Report Details - #") && !title.startsWith(ChatColor.YELLOW.toString() + "Archived Bug Details")) {
				return
			}

			val bugReportID =
				title.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex())
					.dropLastWhile { it.isEmpty() }.toTypedArray()[0]

			event.isCancelled = true

			val player: Player = event.whoClicked as Player
			val clickedItem = event.currentItem
			clickedItem!!

			if (clickedItem.type == Material.AIR) {
				return
			}

			val itemMeta: ItemMeta? = clickedItem.itemMeta
			itemMeta!!

			if (!itemMeta.hasDisplayName() || itemMeta.displayName == " ") {
				return
			}

			val itemName: String = itemMeta.displayName
			val customDisplayName: String = getEnglishValueFromValue(itemName).toString()

			if (debugMode) {
				plugin.logger.info("Clicked item: $customDisplayName")
			}

			if (customDisplayName == " ") {
				return
			}

			if (customDisplayName.contains("(Click to change)")) {
				playButtonClickSound(player)
				player.openInventory(getStatusSelectionGUI(reportIDGUI))
			}

			if (customDisplayName.contains("(Click to teleport)")) {
				playButtonClickSound(player)
				if (debugMode) {
					plugin.logger.info("Teleporting to the location of bug report #$reportIDGUI...")
				}

				if (checkForKey("useTitleInsteadOfMessage", true)) {
					player.sendTitle(
						returnStartingMessage(ChatColor.GREEN) + " Teleporting to the location of Bug Report #" + reportIDGUI + "." + "." + ".",
						" ",
						10,
						70,
						20
					)
				} else {
					player.sendMessage(returnStartingMessage(ChatColor.GREEN) + " Teleporting to the location of Bug Report #" + reportIDGUI + ".")
				}

				val teleportLocation: Location? = getBugReportLocation(reportIDGUI)
				if (teleportLocation != null) {
					player.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
				}
			}

			when (customDisplayName) {
				"Back" -> {
					playButtonClickSound(player)
					player.openInventory(
						if (isArchivedDetails) getArchivedBugReportsGUI(localCurrentPage, player) else getBugReportGUI(
							localCurrentPage, player
						)
					)
				}

				"Unarchive" -> {
					playButtonClickSound(player)
					updateBugReportArchive(reportIDGUI, 0)

					if (debugMode) {
						plugin.logger.info("Unarchiving bug report #$reportIDGUI...")
					}
					player.openInventory(
						if (isArchivedDetails) getArchivedBugReportsGUI(localCurrentPage, player) else getBugReportGUI(
							localCurrentPage, player
						)
					)
					player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + " Bug Report #" + reportIDGUI + " has been unarchived.")

					HandlerList.unregisterAll(this)
				}

				"Archive" -> {
					if (player.hasPermission("bugreport.archive") || player.hasPermission("bugreport.admin")) {
						playButtonClickSound(player)

						if (debugMode) {
							plugin.logger.info("Archiving bug report #$reportIDGUI...")
						}
						getPluginManager().registerEvents(
							BugReportConfirmationGUI.BugReportConfirmationListener(
								gui, reportIDGUI, isArchivedDetails
							), plugin
						)
						BugReportConfirmationGUI.openConfirmationGUI(player, true, bugReportID)
					} else {
						player.closeInventory()
						player.sendMessage(returnStartingMessage(ChatColor.RED) + " You don't have permission to archive bug reports!")
					}
				}

				"Delete" -> {
					if (player.hasPermission("bugreport.delete") || player.hasPermission("bugreport.admin")) {
						playButtonClickSound(player)

						if (debugMode) {
							plugin.logger.info("Opening confirmation GUI for deletion on Bug Report #$reportIDGUI...")
						}
						getPluginManager().registerEvents(
							BugReportConfirmationGUI.BugReportConfirmationListener(
								gui, reportIDGUI, isArchivedDetails
							), plugin
						)
						BugReportConfirmationGUI.openConfirmationGUI(player, false, bugReportID)
					} else {
						player.closeInventory()
						player.sendMessage(returnStartingMessage(ChatColor.RED) + " You don't have permission to delete bug reports!")
					}
				}
			}
			HandlerList.unregisterAll(this)
		}

		val gui: Inventory = gui
	}

	companion object {
		private var reportCategories: List<Category>? = null
		lateinit var bugReports: Map<UUID, ArrayList<String>>

		var debugMode: Boolean = false
		lateinit var config: FileConfiguration

		private lateinit var configFile: File
		private lateinit var language: String

		var endingPluginTitleColor: ChatColor? = null

		lateinit var pluginColor: ChatColor
		lateinit var pluginTitle: String

		var localCurrentPage: Int = 1
		private lateinit var database: BugReportDatabase

		fun setDebugMode(setDebugMode: Int) {
			debugMode = setDebugMode == 1
		}

		private fun extractValidColorCode(input: String): String? {
			var input = input.replace("[^0-9a-fA-F]".toRegex(), "")
			input = input.trim { it <= ' ' }.substring(0, 1)
			return if (input.matches("[0-9a-fA-F]".toRegex())) {
				input
			} else null
		}

		@Contract(pure = true)
		fun returnStartingMessage(defaultColor: ChatColor?): String {
			return "$pluginColor$pluginTitle " + Objects.requireNonNullElse(
				endingPluginTitleColor, defaultColor
			)
		}

		fun checkCategoryConfig(): Boolean {
			if (!config.contains("reportCategories")) {
				plugin.logger.warning(
					getValueFromLanguageFile(
						"missingReportCategoryMessage", "Missing reportCategories in config.yml"
					)
				)
				logErrorMessage(
					getValueFromLanguageFile(
						"missingReportCategoryMessage", "Missing reportCategories in config.yml"
					)
				)
				return false
			}

			val categoryList: List<Map<*, *>> = config.getMapList("reportCategories")
			for (categoryMap in categoryList) {
				val keys: Array<Any?> = categoryMap.keys.toTypedArray()
				val values = categoryMap.values.toTypedArray()

				for (i in keys.indices) {
					if (values[i] == null) {
						plugin.logger.warning(
							getValueFromLanguageFile(
								"missingValueMessage", "Missing '%key%' in reportCategories in config.yml"
							).replace("%key%", keys[i].toString())
						)
						logErrorMessage(
							getValueFromLanguageFile(
								"missingValueMessage", "Missing '%key%' in reportCategories in config.yml"
							).replace("%key%", keys[i].toString())
						)
						return false
					}
				}
			}
			return true
		}

		fun reloadConfig() {
			language = config.getString("language", "en_US").toString()
			loadConfig()
			checkConfig()
		}

		fun loadConfig() {
			configFile = File(plugin.dataFolder, "config.yml")

			if (!configFile.exists()) {
				plugin.saveResource("config.yml", false)
			}

			config = YamlConfiguration.loadConfiguration(configFile)

			language = config.getString("language", "en_US").toString()
		}

		fun checkConfig() {
			val newValues: HashMap<String, Any> = object : HashMap<String, Any>() {
				@Serial
				private val serialVersionUID = -2578293471267967277L

				init {
					put("webhookURL", "https://discord.com/api/webhooks/")
					put("enableDiscordWebhook", true)
					put("enablePluginReportCategoriesGUI", false)
					put("enablePluginReportCategoriesTabComplete", false)
					put("enablePluginReportBook", false)
					put("enableBugReportNotifications", true)
					put("bug-category-tab-complete", true)
					put("language", "en_US")
					put("update-checker", true)
					put("update-checker-join", true)
					put("discordEmbedColor", "Yellow")
					put("discordEmbedTitle", "New Bug Report")
					put("discordEmbedFooter", "Bug Report V1.0.0")
					put("discordEmbedThumbnail", "https://www.spigotmc.org/data/resource_icons/110/110732.jpg")
					put("discordEnableThumbnail", true)
					put("discordEnableUserAuthor", true)
					put("discordIncludeDate", true)
					put("enableBungeeCordSendMessage", true)
					put("enableBungeeCordReceiveMessage", true)
					put("useTitleInsteadOfMessage", false)
					put("enablePlayerHeads", true)
					put("refreshPlayerHeadCache", "1d")
					put("metrics", true)
					put("serverName", "My Server")
					put("max-reports-per-player", 50)
					put("report-confirmation-message", "Thanks for submitting a report!")
					put("bug-report-cooldown", 0)
					put("pluginColor", "Yellow")
					put("pluginTitle", "[Bug Report]")
				}
			}

			for ((key, value1) in newValues) {
				val value = value1
				if (!config.contains(key)) {
					config.set(key, value)
				}
			}
			saveConfig()
		}

		fun saveConfig() {
			if (debugMode) {
				plugin.logger.info("Saving config.yml...")
			}

			try {
				config.save(configFile)
			} catch (e: Exception) {
				plugin.logger.warning("Error saving config.yml: " + e.message)
				logErrorMessage("Error saving config.yml: " + e.message)
			}
		}

		fun generateBugReportGUI(testCurrentPage: Int, player: Player, showArchived: Boolean): Inventory {
			loadBugReports()

			val itemsPerPage = 27
			val navigationRow = 36

			val reports: List<String> = bugReports.getOrDefault(
				getStaticUUID(), ArrayList<String>(
					listOf("DUMMY")
				)
			)

			getScheduler().runTaskAsynchronously(plugin, Runnable {
				for (report in reports) {
					val username = getReportByKey(report, "Username")
					com.bugreportmc.bugreport.api.DataSource.getPlayerHead(username)
				}
			})

			val filteredReports = getFilteredReports(showArchived, reports)

			val totalPages = max(
				1.0, ceil(filteredReports.size.toDouble() / itemsPerPage).toInt().toDouble()
			).toInt()
			val currentPage = max(1.0, min(testCurrentPage.toDouble(), totalPages.toDouble())).toInt()

			val gui: Inventory = createInventory(
				null,
				45,
				ChatColor.YELLOW.toString() + (if (showArchived) "Archived Bugs" else "Bug " + "Report") + " - " + Objects.requireNonNull<String>(
					getValueFromLanguageFile(
						"buttonNames.pageInfo", "Page %currentPage% of %totalPages%"
					)
				).replace("%currentPage%", currentPage.toString()).replace("%totalPages%", totalPages.toString())
			)

			val startIndex = (currentPage - 1) * itemsPerPage
			val endIndex = min((startIndex + itemsPerPage).toDouble(), filteredReports.size.toDouble()).toInt()
			var slotIndex = 0

			for (i in startIndex until endIndex) {
				val report = filteredReports[i]
				val reportID = getReportByKey(report, "Report ID")
				val firstLine = report.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
				val username = firstLine.split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

				val playerHead: ItemStack =
					if (config.getBoolean("enablePlayerHeads")) com.bugreportmc.bugreport.api.DataSource.getPlayerHead(
						username
					) else createInfoItem(
						Material.ENCHANTED_BOOK,
						ChatColor.GOLD.toString() + "Username",
						ChatColor.WHITE.toString() + username,
						false
					)

				val reportItem = ItemStack(playerHead)

				val itemMeta: ItemMeta? = reportItem.itemMeta
				itemMeta?.setDisplayName(ChatColor.YELLOW.toString() + "Bug Report #" + reportID)
				itemMeta?.lore = listOf(ChatColor.GRAY.toString() + firstLine)

				reportItem.setItemMeta(itemMeta)

				gui.setItem(slotIndex, reportItem)
				slotIndex++
			}

			val settingsButton: ItemStack = createButton(
				Material.CHEST, ChatColor.YELLOW.toString() + getValueFromLanguageFile(
					"buttonNames.settings", "Settings"
				)
			)
			val closeButton: ItemStack = createButton(
				Material.BARRIER, ChatColor.RED.toString() + getValueFromLanguageFile("buttonNames.close", "Close")
			)
			val pageIndicator: ItemStack = createButton(
				Material.PAPER, ChatColor.YELLOW.toString() + Objects.requireNonNull<String>(
					getValueFromLanguageFile(
						"buttonNames.pageInfo", "Page %currentPage% of %totalPages%"
					)
				).replace("%currentPage%", currentPage.toString()).replace("%totalPages%", totalPages.toString())
			)

			if (getCurrentPage(player) == 1) {
				gui.setItem(36, ItemStack(Material.AIR))
			} else {
				createNavigationButtons("back", gui, 36)
			}

			if (getCurrentPage(player) == Companion.totalPages) {
				gui.setItem(44, ItemStack(Material.AIR))
			} else {
				createNavigationButtons("forward", gui, 44)
			}

			gui.setItem(navigationRow + 2, settingsButton)
			gui.setItem(navigationRow + 4, pageIndicator)
			gui.setItem(navigationRow + 6, closeButton)

			return gui
		}

		fun getReportByKey(currentReport: String, keyName: String): String? {
			val reportLines = currentReport.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			val reportData: MutableMap<String, String> = HashMap()

			for (line in reportLines) {
				val colonIndex = line.indexOf(":")
				if (colonIndex >= 0) {
					val key = line.substring(0, colonIndex).trim { it <= ' ' }
					val value = line.substring(colonIndex + 1).trim { it <= ' ' }
					reportData[key] = value
				}
			}

			return reportData[keyName]
		}

		fun playButtonClickSound(player: Player) {
			player.playSound(player.location, "ui.button.click", 0.6f, 1.0f)
		}

		private fun createNavigationButtons(forward: String, bugReportGUI: Inventory, index: Int) {
			val forwardButton = ItemStack(Material.ARROW)
			val forwardMeta: ItemMeta? = forwardButton.itemMeta
			Objects.requireNonNull<ItemMeta>(forwardMeta).setDisplayName(
				ChatColor.GREEN.toString() + getValueFromLanguageFile(
					"buttonNames.$forward",
					forward.substring(0, 1).uppercase(Locale.getDefault()) + forward.substring(1)
				)
			)
			forwardButton.setItemMeta(forwardMeta)
			bugReportGUI.setItem(index, forwardButton)
		}

		private fun getFilteredReports(showArchived: Boolean, reports: List<String?>): List<String> {
			val filteredReports: MutableList<String> = ArrayList()
			for (report in reports) {
				if (showArchived && report!!.contains("Archived: 1") || (!showArchived && !report!!.contains("DUMMY") && !report.contains(
						"Archived: 1"
					))
				) {
					filteredReports.add(report)
				}
			}

			filteredReports.sortWith { r1: String, r2: String ->
				val id1 = extractReportIDFromReport(r1).toInt()
				val id2 = extractReportIDFromReport(r2).toInt()
				id1.compareTo(id2)
			}
			return filteredReports
		}

		private fun extractReportIDFromReport(report: String): String {
			val reportLines = report.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			for (line in reportLines) {
				val colonIndex = line.indexOf(":")
				if (colonIndex >= 0) {
					val key = line.substring(0, colonIndex).trim { it <= ' ' }
					val value = line.substring(colonIndex + 1).trim { it <= ' ' }
					if ("Report ID" == key) {
						return value
					}
				}
			}
			return "0"
		}

		fun getArchivedBugReportsGUI(testCurrentPage: Int, player: Player): Inventory {
			return generateBugReportGUI(testCurrentPage, player, true)
		}

		fun getBugReportGUI(testCurrentPage: Int, player: Player): Inventory {
			return generateBugReportGUI(testCurrentPage, player, false)
		}

		fun createButton(material: Material, name: String): ItemStack {
			val item = ItemStack(material)
			val meta = item.itemMeta!!

			meta.setDisplayName(name)
			item.setItemMeta(meta)
			return item
		}

		private fun loadBugReports() {
			bugReports = BugReportDatabase.loadBugReports()
		}

		fun getCurrentPage(player: Player): Int {
			val metadata: List<MetadataValue> = player.getMetadata("currentPage")
			if (debugMode) {
				plugin.logger.info("Current page for " + player.name + " is " + (if (metadata.isNotEmpty()) metadata[0].asInt() else 0))
			}
			return if (metadata.isNotEmpty()) metadata[0].asInt() else 0
		}

		fun setCurrentPage(player: Player, page: Int) {
			if (debugMode) {
				plugin.logger.info("Setting current page to " + page + " for " + player.name)
			}
			player.setMetadata("currentPage", FixedMetadataValue(plugin, page))
		}

		val totalPages: Int
			get() {
				val reports: List<String?> = bugReports.getOrDefault(
					getStaticUUID(), ArrayList<String?>(
						listOf<String>("DUMMY")
					)
				)
				return ceil(reports.size.toDouble() / 27).toInt()
			}

		/**
		 * Check if a key exists in the config, and if it does, return the value.
		 * If checkForBoolean is true, return the value.
		 * If checkForBoolean is false, return if the key exists.
		 */
		fun checkForKey(key: String, checkForBoolean: Boolean): Boolean {
			if (!config.contains(key) || config.get(key) == null) {
				return false
			}

			return !checkForBoolean || config.getBoolean(key)
		}

		fun translateTimestampToDate(timestamp: Long): String {
			val date = Date(timestamp)
			val calendar: Calendar = Calendar.getInstance()
			calendar.time = date

			val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
			val hour: Int = calendar.get(Calendar.HOUR)
			val minute: Int = calendar.get(Calendar.MINUTE)

			val daySuffix = getDayOfMonthSuffix(day)
			val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
			var hourString = hour.toString()
			var minuteString = minute.toString()

			if (hour < 10) hourString = "0$hourString"
			if (minute < 10) minuteString = "0$minuteString"

			return StringJoiner(" ").add(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH))
				.add("$day$daySuffix,").add(calendar.get(Calendar.YEAR).toString()).add("at").add(
					"$hourString:$minuteString"
				).add(amPm).toString()
		}

		@Contract(pure = true)
		private fun getDayOfMonthSuffix(day: Int): String {
			return when (day) {
				1, 21, 31 -> "st"
				2, 22 -> "nd"
				3, 23 -> "rd"
				else -> "th"
			}
		}

		fun createEmptyItem(): ItemStack {
			val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
			val meta: ItemMeta = item.itemMeta!!
			Objects.requireNonNull(meta).setDisplayName(" ")
			item.setItemMeta(meta)

			return item
		}

		fun createInfoItem(material: Material, name: String?, value: String, longMessage: Boolean): ItemStack {
			val item = ItemStack(material)
			val meta = item.itemMeta
			Objects.requireNonNull<ItemMeta>(meta).setDisplayName(name)

			if (longMessage) {
				val lore: MutableList<String> = ArrayList()
				val words = value.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				var currentLine = StringBuilder()
				for (word in words) {
					if (currentLine.length + word.length > 30) {
						lore.add(currentLine.toString())
						currentLine = StringBuilder()
					}

					currentLine.append(word).append(" ")
				}

				if (currentLine.isNotEmpty()) {
					lore.add(currentLine.toString())
				}
				meta?.lore = lore
			} else {
				meta?.lore = listOf(value)
			}

			item.setItemMeta(meta)
			return item
		}

		fun getReportCategories(): List<Category> {
			return reportCategories!!
		}
	}
}
