package com.bugreportmc.bugreport.commands

import com.bugreportmc.bugreport.BugReportDatabase.Companion.getStaticUUID
import com.bugreportmc.bugreport.BugReportLanguage.Companion.getValueFromLanguageFile
import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportManager.Companion.checkCategoryConfig
import com.bugreportmc.bugreport.BugReportManager.Companion.checkForKey
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginColor
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginTitle
import com.bugreportmc.bugreport.BugReportManager.Companion.returnStartingMessage
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.Category
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.annotations.Contract
import java.awt.Color
import java.util.*

class BugReportCommand(private val reportManager: BugReportManager) : CommandExecutor, Listener {
	private val categorySelectionMap: MutableMap<UUID, Int> = HashMap()
	private val lastCommandUsage: MutableMap<UUID, Long> = HashMap()

	@EventHandler
	fun onBookEdit(event: PlayerEditBookEvent) {
		val player = event.player
		val bookMeta = event.newBookMeta
		val isSigning = event.isSigning

		if (isSigning) {
			if (!bookMeta.hasCustomModelData() || bookMeta.customModelData != 1889234213) {
				return
			}
			val pages = bookMeta.pages
			val content = java.lang.String.join(" ", pages)
			reportManager.submitBugReport(player, content, 0)
			player.sendMessage(
				returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
					"bugReportConfirmationMessage", "Bug report submitted successfully!"
				)
			)

			object : BukkitRunnable() {
				override fun run() {
					var foundAndRemoved = false
					val contents = player.inventory.contents
					for (item in contents) {
						if (item != null && item.hasItemMeta() && item.itemMeta is BookMeta) {
							if (bookMeta.hasCustomModelData() && bookMeta.customModelData == 1889234213 && (item.type == Material.WRITTEN_BOOK || item.type == Material.WRITABLE_BOOK)) {
								player.inventory.remove(item)
								player.updateInventory()
								doubleCheckIfBookWasRemoved(player)
								foundAndRemoved = true
								break
							}
						}
					}
					if (!foundAndRemoved) {
						plugin.logger.warning("Logging: Failed to find and remove book for player " + player.name)
						logErrorMessage("Logging: Failed to find and remove book for player " + player.name)
					} else {
						plugin.logger.info("Logging: Removed book for player " + player.name)
					}
				}
			}.runTaskLater(plugin, 1L)
		}
	}

	private fun doubleCheckIfBookWasRemoved(player: Player) {
		object : BukkitRunnable() {
			override fun run() {
				var foundBook = false
				val contents: Array<ItemStack?> = player.inventory.contents
				for (item in contents) {
					if (item != null && item.hasItemMeta()) {
						val meta = item.itemMeta as? BookMeta
						if (meta != null && meta.hasCustomModelData() && meta.customModelData == 1889234213 && (item.type == Material.WRITTEN_BOOK || item.type == Material.WRITABLE_BOOK)) {
							foundBook = true
							player.inventory.remove(item)
							player.updateInventory()
							break
						}
					}
				}
				if (foundBook) {
					plugin.logger.warning("Logging: Failed to remove book for player " + player.name)
					logErrorMessage("Logging: Failed to remove book for player " + player.name)
				} else {
					plugin.logger.info("Logging: Removed book for player " + player.name)
				}
			}
		}.runTaskLater(plugin, 20L)
	}

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage("This command can only be run by a player.")
			return true
		}

		val cooldown = BugReportManager.config.getInt("bug-report-cooldown", 0)
		if (cooldown > 0) {
			val currentTime = System.currentTimeMillis()
			val lastUsage = lastCommandUsage.getOrDefault(sender.uniqueId, 0L)
			val timeElapsed = (currentTime - lastUsage) / 1000

			if (timeElapsed < cooldown) {
				val timeLeft = cooldown - timeElapsed
				sender.sendMessage(returnStartingMessage(ChatColor.RED) + "You must wait " + timeLeft + " seconds before using this command again.")
				return true
			}
		}

		if (sender.hasPermission("bugreport.use") || sender.hasPermission("bugreport.admin")) {
			if (BugReportManager.config.getBoolean("enablePluginReportBook", true)) {
				val bugReportBook = ItemStack(Material.WRITABLE_BOOK)
				val meta = bugReportBook.itemMeta as BookMeta?

				if (meta != null) {
					meta.setCustomModelData(1889234213)
					meta.setDisplayName(ChatColor.YELLOW.toString() + "Bug Report")
					meta.setTitle("Bug Report")
					meta.author = sender.getName()
					meta.addPage("Write your bug report here...")
					bugReportBook.setItemMeta(meta)
				}

				if (sender.inventory.firstEmpty() == -1) {
					sender.sendMessage(returnStartingMessage(ChatColor.RED) + "Your inventory is full, please make some space before getting a bug report book")
					return true
				}

				sender.inventory.addItem(bugReportBook)
				val message =
					"$pluginColor$pluginTitle ${ChatColor.YELLOW}Bug Report book added to your inventory$pluginColor$pluginTitle ${ChatColor.YELLOW}Write your bug report in the book and sign it to submit"

				sender.sendMessage(message)
				lastCommandUsage[sender.uniqueId] = System.currentTimeMillis()
				return true
			}
			if (BugReportManager.config.getBoolean("enablePluginReportCategoriesGUI", true)) {
				if (!checkCategoryConfig()) {
					sender.sendMessage(
						returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
							"bugReportCategoriesNotConfiguredMessage", "Bug report categories are not configured"
						)
					)
					return true
				}
				openCategorySelectionGUI(sender)
				lastCommandUsage[sender.uniqueId] = System.currentTimeMillis()
				return true
			}

			if (args.isEmpty()) {
				sender.sendMessage(returnStartingMessage(ChatColor.RED) + "Usage: /bugreport <message>")
				return true
			}

			val maxReports = BugReportManager.config.getInt("max-reports-per-player")
			if (maxReports != 0) {
				val reportsLeft = maxReports - getReportCount(sender.uniqueId)
				if (reportsLeft <= 0) {
					if (checkForKey("useTitleInsteadOfMessage", true)) {
						sender.sendTitle(
							ChatColor.RED.toString() + getValueFromLanguageFile(
								"maxReportsPerPlayerMessage",
								"You have reached the maximum amount of reports you can submit"
							), "", 10, 70, 25
						)
					} else {
						sender.sendMessage(
							returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
								"maxReportsPerPlayerMessage",
								"You have reached the maximum amount of reports you can submit"
							)
						)
					}
					return true
				}
			}

			try {
				reportManager.submitBugReport(sender, java.lang.String.join(" ", *args), 0)
			} catch (e: Exception) {
				plugin.logger.warning("Failed to submit bug report")
				logErrorMessage("Failed to submit bug report")
				throw RuntimeException(e)
			}

			if (checkForKey("useTitleInsteadOfMessage", true)) {
				sender.sendTitle(
					ChatColor.GREEN.toString() + getValueFromLanguageFile(
						"bugReportConfirmationMessage", "Bug report submitted successfully!"
					), "", 10, 70, 25
				)
			} else {
				sender.sendMessage(
					returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
						"bugReportConfirmationMessage", "Bug report submitted successfully!"
					)
				)
			}
			lastCommandUsage[sender.uniqueId] = System.currentTimeMillis()
		} else {
			sender.sendMessage(
				"$pluginColor$pluginTitle " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.RED
				) + "You don't have permission to use this command!"
			)
		}

		if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
			sender.sendMessage(
				"$pluginColor$pluginTitle " + Objects.requireNonNullElse(
					BugReportManager.endingPluginTitleColor, ChatColor.GREEN
				) + "Commands:"
			)
			sender.sendMessage(ChatColor.GOLD.toString() + "/bugreport <Message>" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Submits a bug report.")
			sender.sendMessage(ChatColor.GOLD.toString() + "/bugreport help" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Displays this help message.")
			return true
		}

		return true
	}

	private fun getReportCount(playerId: UUID): Int {
		var count = 0
		val reports: List<String?> = BugReportManager.bugReports.getOrDefault(getStaticUUID(), ArrayList(listOf("DUMMY")))
		for (report in reports) {
			if (report!!.contains(playerId.toString())) {
				count++
			}
		}
		return count
	}

	private fun openCategorySelectionGUI(player: Player) {
		val gui = Bukkit.createInventory(null, 9, ChatColor.YELLOW.toString() + "Bug Report Categories")

		val categories = reportManager.reportCategories

		for (category in categories!!) {
			val categoryItem = createCategoryItem(category)
			gui.addItem(categoryItem)
		}

		player.openInventory(gui)
	}

	@EventHandler(priority = EventPriority.NORMAL)
	fun onInventoryClick(event: InventoryClickEvent) {
		if (event.view.title != ChatColor.YELLOW.toString() + "Bug Report Categories") {
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

		val categoryName = ChatColor.stripColor(itemMeta.displayName)
		val categories = reportManager.reportCategories
		var selectedCategory: Category? = null

		for (category in categories!!) {
			if (category.name == categoryName) {
				selectedCategory = category
				break
			}
		}

		if (selectedCategory != null) {
			categorySelectionMap[player.uniqueId] = selectedCategory.id
			player.closeInventory()
			if (checkForKey("useTitleInsteadOfMessage", true)) {
				player.sendTitle(
					ChatColor.YELLOW.toString() + getValueFromLanguageFile(
						"enterBugReportMessageCategory", "Please enter your bug report in chat. Type 'cancel' to cancel"
					), "", 10, 70, 120
				)
			} else {
				player.sendMessage(
					returnStartingMessage(ChatColor.YELLOW) + getValueFromLanguageFile(
						"enterBugReportMessageCategory", "Please enter your bug report in chat. Type 'cancel' to cancel"
					)
				)
			}
		} else {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + "Something went wrong while selecting the category")
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	fun onPlayerChat(event: AsyncPlayerChatEvent) {
		val player = event.player
		val categoryId = categorySelectionMap[player.uniqueId] ?: return

		event.isCancelled = true
		categorySelectionMap.remove(player.uniqueId)
		val message = event.message

		if (message.equals("cancel", ignoreCase = true)) {
			if (checkForKey("useTitleInsteadOfMessage", true)) {
				player.sendTitle(
					ChatColor.RED.toString() + getValueFromLanguageFile(
						"cancelledBugReportMessage", "Bug report cancelled"
					), "", 10, 70, 25
				)
			} else {
				player.sendMessage(
					returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile(
						"cancelledBugReportMessage", "Bug report cancelled"
					)
				)
			}
			return
		}

		reportManager.submitBugReport(player, message, categoryId)
		if (checkForKey("useTitleInsteadOfMessage", true)) {
			player.sendTitle(
				ChatColor.GREEN.toString() + getValueFromLanguageFile(
					"bugReportConfirmationMessage", "Bug report submitted successfully!"
				), "", 10, 70, 25
			)
		} else {
			player.sendMessage(
				returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile(
					"bugReportConfirmationMessage", "Bug report submitted successfully!"
				)
			)
		}
	}

	private fun createCategoryItem(category: Category): ItemStack {
		val itemStack = ItemStack(category.item)
		val itemMeta = itemStack.itemMeta
		itemMeta!!.setDisplayName(stringColorToColorCode(category.color).toString() + category.name)
		itemMeta.lore = listOf(ChatColor.GRAY.toString() + category.description)
		itemStack.setItemMeta(itemMeta)
		return itemStack
	}

	companion object {
		@Contract(pure = true)
		fun stringColorToColorCode(color: String?): ChatColor {
			if (color == null) {
				return ChatColor.WHITE
			}
			return when (color.uppercase(Locale.getDefault())) {
				"AQUA" -> ChatColor.AQUA
				"BLACK" -> ChatColor.BLACK
				"BLUE" -> ChatColor.BLUE
				"DARK_AQUA" -> ChatColor.DARK_AQUA
				"DARK_BLUE" -> ChatColor.DARK_BLUE
				"DARK_GRAY" -> ChatColor.DARK_GRAY
				"DARK_GREEN" -> ChatColor.DARK_GREEN
				"DARK_PURPLE" -> ChatColor.DARK_PURPLE
				"DARK_RED" -> ChatColor.DARK_RED
				"GOLD" -> ChatColor.GOLD
				"GRAY" -> ChatColor.GRAY
				"GREEN" -> ChatColor.GREEN
				"LIGHT_PURPLE" -> ChatColor.LIGHT_PURPLE
				"RED" -> ChatColor.RED
				"WHITE" -> ChatColor.WHITE
				"YELLOW" -> ChatColor.YELLOW
				else -> ChatColor.WHITE
			}
		}

		fun checkIfChatColorIsValid(chatColor: String): Boolean {
			return when (chatColor.uppercase(Locale.getDefault())) {
				"AQUA", "BLACK", "BLUE", "DARK_AQUA", "DARK_BLUE", "DARK_GRAY", "DARK_GREEN", "DARK_PURPLE",
				"DARK_RED", "GOLD", "GRAY", "GREEN", "LIGHT_PURPLE", "RED", "WHITE", "YELLOW",
				-> true

				else -> false
			}
		}

		@Contract(pure = true)
		fun getChatColorByCode(colorCode: String): ChatColor {
			return when (colorCode) {
				"§0" -> ChatColor.BLACK
				"§1" -> ChatColor.DARK_BLUE
				"§2" -> ChatColor.DARK_GREEN
				"§3" -> ChatColor.DARK_AQUA
				"§4" -> ChatColor.DARK_RED
				"§5" -> ChatColor.DARK_PURPLE
				"§6" -> ChatColor.GOLD
				"§7" -> ChatColor.GRAY
				"§8" -> ChatColor.DARK_GRAY
				"§9" -> ChatColor.BLUE
				"§a" -> ChatColor.GREEN
				"§b" -> ChatColor.AQUA
				"§c" -> ChatColor.RED
				"§d" -> ChatColor.LIGHT_PURPLE
				"§e" -> ChatColor.YELLOW
				"§f" -> ChatColor.WHITE
				"§k" -> ChatColor.MAGIC
				"§l" -> ChatColor.BOLD
				"§m" -> ChatColor.STRIKETHROUGH
				"§n" -> ChatColor.UNDERLINE
				"§o" -> ChatColor.ITALIC
				"§r" -> ChatColor.RESET
				else -> throw IllegalArgumentException("Invalid color code: $colorCode")
			}
		}

		@Contract(pure = true)
		fun chatColorToColor(color: ChatColor?): Color {
			if (color == null) {
				return Color.WHITE
			}
			return when (color) {
				ChatColor.AQUA -> Color.CYAN
				ChatColor.BLACK -> Color.BLACK
				ChatColor.BLUE -> Color.BLUE
				ChatColor.DARK_AQUA -> Color.CYAN.darker()
				ChatColor.DARK_BLUE -> Color.BLUE.darker()
				ChatColor.DARK_GRAY -> Color.GRAY.darker()
				ChatColor.DARK_GREEN -> Color.GREEN.darker()
				ChatColor.DARK_PURPLE -> Color.MAGENTA.darker()
				ChatColor.DARK_RED -> Color.RED.darker()
				ChatColor.GOLD -> Color.ORANGE
				ChatColor.GRAY -> Color.GRAY
				ChatColor.GREEN -> Color.GREEN
				ChatColor.LIGHT_PURPLE -> Color.MAGENTA
				ChatColor.RED -> Color.RED
				ChatColor.WHITE -> Color.WHITE
				ChatColor.YELLOW -> Color.YELLOW
				else -> Color.WHITE
			}
		}
	}
}
