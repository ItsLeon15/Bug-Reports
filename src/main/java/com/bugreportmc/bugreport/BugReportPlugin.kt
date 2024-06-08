package com.bugreportmc.bugreport

import com.bugreportmc.bugreport.BugReportDatabase.Companion.getPlayerLastLoginTimestamp
import com.bugreportmc.bugreport.BugReportDatabase.Companion.getStaticUUID
import com.bugreportmc.bugreport.BugReportLanguage.Companion.getValueFromLanguageFile
import com.bugreportmc.bugreport.BugReportManager.Companion.bugReports
import com.bugreportmc.bugreport.BugReportManager.Companion.debugMode
import com.bugreportmc.bugreport.BugReportManager.Companion.endingPluginTitleColor
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginColor
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginTitle
import com.bugreportmc.bugreport.api.CacheCleanupListener
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.commands.*
import com.bugreportmc.bugreport.expansions.BugPlaceholders
import com.bugreportmc.bugreport.extensions.PlanHook
import com.bugreportmc.bugreport.gui.bugreportGUI
import com.bugreportmc.bugreport.listeners.ItemDropEvent
import com.bugreportmc.bugreport.listeners.PluginMessageListener
import com.bugreportmc.bugreport.listeners.ReportListener
import com.bugreportmc.bugreport.listeners.UpdateChecker
import me.clip.placeholderapi.metrics.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.ChatColor
import org.bukkit.command.PluginCommand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.math.min

class BugReportPlugin : JavaPlugin(), Listener {
	private val updateChecker: UpdateChecker = UpdateChecker(this, 110732)
	private val notifiedPlayers: MutableSet<UUID> = HashSet<UUID>()
	private lateinit var reportManager: BugReportManager

	override fun onEnable() {
		plugin = this
		reportManager = BugReportManager(this)

		try {
			PlanHook.instance?.hookIntoPlan()
		} catch (planIsNotInstalled: NoClassDefFoundError) {
			// Ignore catch
		}

		if (getPluginManager().getPlugin("PlaceholderAPI") != null) {
			BugPlaceholders(this).register()
		}

		if (!dataFolder.exists()) {
			if (!dataFolder.mkdirs()) {
				plugin.logger.warning("Failed to create data folder.")
				logErrorMessage("Failed to create data folder.")
			}
		}

		if (getConfig().getBoolean("update-checker")) {
			updateChecker.getVersion { spigotVersion: String ->
				val serverVersion: String = this.description.version
				if (compareVersions(serverVersion, spigotVersion) < 0) {
					plugin.logger.info("A new version of Bug Report is available: $spigotVersion")
				}
			}
		}

		registerCommands()
		registerListeners()

		BugReportLanguage(this)

		if (getConfig().getBoolean("metrics")) {
			Metrics(this, 18974)
		}

		bugreportGUI.generateNewYML()
	}

	override fun onDisable() {
		bugReports.forEach { (_, value) -> value.clear() }

		try {
			BugReportDatabase.dataSource.close()
		} catch (e: Exception) {
			plugin.logger.warning("Failed to close database connection.")
			logErrorMessage("Failed to close database connection.")
		}

		this.server.messenger.unregisterOutgoingPluginChannel(this)
		this.server.messenger.unregisterIncomingPluginChannel(this)
	}

	@EventHandler
	fun onPlayerLeave(event: PlayerQuitEvent) {
		val player: Player = event.player
		val playerId: UUID = player.uniqueId
		BugReportDatabase.setPlayerLastLoginTimestamp(playerId)
	}

	private fun compareVersions(version1: String, version2: String): Int {
		if (debugMode) {
			plugin.logger.info("Comparing versions: $version1 and $version2")
		}
		val parts1 = version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val parts2 = version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

		val minLength = min(parts1.size.toDouble(), parts2.size.toDouble()).toInt()

		for (i in 0 until minLength) {
			val part1 = parts1[i].toInt()
			val part2 = parts2[i].toInt()

			if (part1 < part2) {
				return -1
			} else if (part1 > part2) {
				return 1
			}
		}

		if (parts1.size < parts2.size) {
			return -1
		} else if (parts1.size > parts2.size) {
			return 1
		}

		return 0
	}

	@EventHandler
	fun onPlayerJoin(event: PlayerJoinEvent) {
		for (onlinePlayer in Bukkit.getOnlinePlayers()) {
			if (onlinePlayer.isOp || onlinePlayer.hasPermission("bugreport.notify")) {
				val playerId: UUID = onlinePlayer.uniqueId

				if (!config.getBoolean("enableBugReportNotifications")) {
					continue
				}

				if (notifiedPlayers.contains(playerId)) {
					return
				}

				val lastLoginTimestamp: Long = getPlayerLastLoginTimestamp(playerId)
				val reports: ArrayList<String> = bugReports.getOrDefault(
					getStaticUUID(), ArrayList<String>(
						listOf("DUMMY")
					)
				)
				val newReports = getNewReports(reports, lastLoginTimestamp)

				val message = StringBuilder()
				if (newReports.isNotEmpty()) {
					message.append(pluginColor).append(pluginTitle).append(" ").append(
						Objects.requireNonNullElse<ChatColor>(
							endingPluginTitleColor, ChatColor.GRAY
						)
					).append(
						getValueFromLanguageFile(
							"newReportsMessage", "You have %numReports% new reports"
						).replace("%numReports%", newReports.size.toString())
					).append("\n")
				} else {
					message.append(pluginColor).append(pluginTitle).append(" ").append(
						Objects.requireNonNullElse<ChatColor>(
							endingPluginTitleColor, ChatColor.GRAY
						)
					).append(
						getValueFromLanguageFile(
							"noNewReportsMessage", "You have no new reports"
						)
					).append("\n")
				}

				if (getConfig().getBoolean("update-checker-join")) {
					updateChecker.getVersion { spigotVersion: String ->
						val serverVersion: String = this.description.version
						if (compareVersions(serverVersion, spigotVersion) < 0) {
							message.append(pluginColor).append(pluginTitle).append(" ").append(
								Objects.requireNonNullElse<ChatColor>(
									endingPluginTitleColor, ChatColor.GRAY
								)
							).append("A new version of Bug Report is available:").append(ChatColor.YELLOW).append(" v")
								.append(spigotVersion)
							onlinePlayer.sendMessage(message.toString())
						}
					}
				} else {
					onlinePlayer.sendMessage(message.toString())
				}

				notifiedPlayers.add(playerId)
			}
		}
	}

	private fun registerCommands() {
		if (debugMode) {
			plugin.logger.info("Registering commands")
		}

		this.saveDefaultConfig()

		val bugReportCommandExecutor = BugReportCommand(reportManager)
		val universalTabCompleter = UniversalTabCompleter(this.getConfig())

		getCommand("buglistarchived")?.setExecutor(BugListArchivedCommand())
		getCommand("buglistsettings")?.setExecutor(BugListSettingsCommand())
		getCommand("buglinkdiscord")?.setExecutor(LinkDiscordCommand(reportManager))

		val bugReportCommand: PluginCommand = Objects.requireNonNull<PluginCommand>(this.getCommand("bugreport"))
		val bugListCommand: PluginCommand = Objects.requireNonNull<PluginCommand>(this.getCommand("buglist"))

		bugReportCommand.tabCompleter = universalTabCompleter
		bugListCommand.tabCompleter = universalTabCompleter

		bugReportCommand.setExecutor(bugReportCommandExecutor)
		bugListCommand.setExecutor(BugListCommand())
	}

	private fun registerListeners() {
		server.pluginManager.registerEvents(BugReportSettings.BugReportSettingsListener(), this)
		server.pluginManager.registerEvents(BugReportManager.BugReportListener(), this)
		server.pluginManager.registerEvents(BugReportCommand(reportManager), this)
		server.pluginManager.registerEvents(ItemDropEvent(), this)
		server.pluginManager.registerEvents(ReportListener(), this)
		server.pluginManager.registerEvents(this, this)

		server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
		server.messenger.registerIncomingPluginChannel(this, "BungeeCord", PluginMessageListener())

		CacheCleanupListener()
	}

	companion object {
		lateinit var plugin: Plugin

		private fun getNewReports(reports: ArrayList<String>, lastLoginTimestamp: Long): ArrayList<String> {
			val newReports: ArrayList<String> = ArrayList()
			for (report in reports) {
				val lines = report.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				var reportTimestampLong: Long = 0
				for (line in lines) {
					if (line.startsWith("Timestamp:")) {
						reportTimestampLong = line.substring(10).trim { it <= ' ' }.toLong()
					}
				}
				if (reportTimestampLong > lastLoginTimestamp) {
					newReports.add(report)
				}
			}
			return newReports
		}
	}
}
