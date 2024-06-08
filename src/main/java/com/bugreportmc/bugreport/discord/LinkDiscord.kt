package com.bugreportmc.bugreport.discord

import com.bugreportmc.bugreport.api.ErrorClass
import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportManager.Companion.config
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.commands.BugReportCommand
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getLogger
import java.awt.Color
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LinkDiscord(private var webhookURL: String) {
	fun setWebhookURL(webhookURL: String) {
		this.webhookURL = webhookURL
	}

	private fun generateDefaultEmbed(): DiscordWebhook.EmbedObject {
		var discordEmbedTitle = config.getString("discordEmbedTitle")
		var discordEmbedFooter = config.getString("discordEmbedFooter")
		var discordEmbedThumbnail = config.getString("discordEmbedThumbnail")
		var discordEmbedColor = BugReportCommand.chatColorToColor(
			BugReportCommand.stringColorToColorCode(
				config.getString("discordEmbedColor")
			)
		)

		discordEmbedFooter = if (discordEmbedFooter.isNullOrEmpty()) EMBED_FOOTER_TEXT else discordEmbedFooter
		discordEmbedColor = discordEmbedColor ?: EMBED_COLOR
		discordEmbedTitle = if (discordEmbedTitle.isNullOrEmpty()) EMBED_TITLE else discordEmbedTitle
		discordEmbedThumbnail = if (discordEmbedThumbnail.isNullOrEmpty()) EMBED_THUMBNAIL else discordEmbedThumbnail

		return DiscordWebhook.EmbedObject().setTitle(discordEmbedTitle).setFooter(discordEmbedFooter, "")
			.setColor(discordEmbedColor).setThumbnail(discordEmbedThumbnail)
	}

	private fun sendEmptyEmbedOrDefault(username: String, vararg existingEmbedObject: DiscordWebhook.EmbedObject) {
		val embedObject = if (existingEmbedObject.isNotEmpty()) existingEmbedObject[0] else generateDefaultEmbed()

		val discordEnableUserAuthor = config.getString("discordEnableUserAuthor")!!
		val discordIncludeDate = config.getString("discordIncludeDate")!!
		val discordEnableThumbnail = config.getString("discordEnableThumbnail")!!
		val discordEmbedThumbnail = config.getString("discordEmbedThumbnail")!!
		val userAuthorURL = "https://crafatar.com/avatars/" + getUserIDFromAPI(username)

		if (discordEnableUserAuthor == "true") {
			embedObject.setAuthor(username, userAuthorURL, userAuthorURL)
		}

		if (discordIncludeDate == "true") {
			embedObject.setTimestamp()
		}

		if (discordEnableThumbnail == "true") {
			embedObject.setThumbnail(discordEmbedThumbnail)
		}

		sendEmbed(embedObject)
	}

	fun sendBugReport(
		message: String,
		world: String,
		username: String,
		location: String,
		gamemode: String,
		category: Int,
		serverName: String,
	) {
		if (webhookURL.isEmpty()) {
			plugin.logger.info("Webhook URL is not configured. Bug report not sent to Discord.")
			return
		}

		if (!config.contains("discordEmbedFields")) {
			plugin.logger.warning("discordEmbedFields key is not present in the config. Sending an empty embed.")
			logErrorMessage("discordEmbedFields key is not present in the config. Sending an empty embed.")
		}

		val discordEmbedFields = config.getMapList("discordEmbedFields")
		if (discordEmbedFields.isEmpty()) {
			plugin.logger.warning("discordEmbedFields is empty in the config. Bug report not sent to Discord.")
			logErrorMessage("discordEmbedFields is empty in the config. Bug report not sent to Discord.")
			sendEmptyEmbedOrDefault(username)
			return
		}

		sendDiscordMessageEmbedFull(
			message, world, username, location, gamemode, category, serverName, discordEmbedFields
		)
	}

	private fun sendDiscordMessageEmbedFull(
		message: String,
		world: String,
		username: String,
		location: String,
		gamemode: String,
		category: Int,
		serverName: String,
		discordEmbedFields: List<Map<*, *>>,
	) {
		val discordDetails: MutableList<DiscordEmbedDetails> = ArrayList()

		for (field in discordEmbedFields) {
			val name = field["name"] as String
			val id = field["id"] as Int
			val value = field["value"] as String
			val inline = field["inline"] as Boolean
			discordDetails.add(DiscordEmbedDetails(name, id, value, inline))
		}

		discordDetails.sortWith(Comparator.comparingInt { obj: DiscordEmbedDetails -> obj.id })

		val embedObject = generateDefaultEmbed()

		for (detail in discordDetails) {
			val name = detail.name
			val detailValue = detail.value

			val value =
				getValueForField(detailValue, username, world, location, gamemode, category, message, serverName)

			val inline = detail.inline
			embedObject.addField(name, value, inline)
		}

		sendEmptyEmbedOrDefault(username, embedObject)
	}

	private fun getValueForField(
		fieldValue: String,
		username: String,
		world: String,
		location: String,
		gamemode: String,
		category: Int,
		message: String,
		serverName: String,
	): String {
		var fieldValue = fieldValue
		val player = Bukkit.getPlayer(username)

		if (player != null && PlaceholderAPI.containsPlaceholders(fieldValue)) {
			fieldValue = PlaceholderAPI.setPlaceholders(player, fieldValue)
		}

		val replacements: MutableMap<String, String?> = HashMap()
		replacements["%report_username%"] = username
		replacements["%report_uuid%"] = getUserIDFromAPI(username)
		replacements["%report_world%"] = world
		replacements["%report_location%"] = location
		replacements["%report_status%"] = "Active"
		replacements["%report_gamemode%"] = gamemode
		replacements["%report_category%"] = getCategoryName(category)
		replacements["%report_server_name%"] = serverName
		replacements["%report_full_message%"] = message

		for ((key, value) in replacements) {
			fieldValue = fieldValue.replace(key, value!!)
		}

		return fieldValue
	}

	private fun sendEmbed(embedObject: DiscordWebhook.EmbedObject) {
		val webhook = DiscordWebhook(webhookURL)
		webhook.addEmbed(embedObject)

		if (config.getBoolean("discordEnablePing")) {
			try {
				val discordPingMembers = config.getStringList("discordPingMembers")
				val discordPingRoles = config.getStringList("discordPingRoles")

				val membersToPing = StringBuilder()
				val rolesToPing = StringBuilder()

				if (discordPingMembers.isNotEmpty()) {
					for (member in discordPingMembers) {
						val trimmedMember = member.trim { it <= ' ' }
						if (trimmedMember.isNotEmpty() && trimmedMember != "<@>" && trimmedMember != "@") {
							if (!trimmedMember.startsWith("<@")) {
								membersToPing.append("<@").append(trimmedMember).append(">")
							} else {
								membersToPing.append(trimmedMember)
							}
							membersToPing.append(" ")
						}
					}
				}

				if (discordPingRoles.isNotEmpty()) {
					for (role in discordPingRoles) {
						val trimmedRole = role.trim { it <= ' ' }
						if (trimmedRole.isNotEmpty() && trimmedRole != "<@&>" && trimmedRole != "&") {
							if (!trimmedRole.startsWith("<@&")) {
								rolesToPing.append("<@&").append(trimmedRole).append(">")
							} else {
								rolesToPing.append(trimmedRole)
							}
							rolesToPing.append(" ")
						}
					}
				}

				val content = StringBuilder()
				if (rolesToPing.isNotEmpty() && rolesToPing.toString().contains("<@&") && rolesToPing.toString()
						.contains(">")
				) {
					content.append(rolesToPing.toString().trim { it <= ' ' }).append(" ")
				}
				if (membersToPing.isNotEmpty() && membersToPing.toString().contains("<@") && membersToPing.toString()
						.contains(">")
				) {
					content.append(membersToPing.toString().trim { it <= ' ' }).append(" ")
				}

				if (content.isNotEmpty()) {
					if (config.getString("discordPingMessage") != null && config.getString(
							"discordPingMessage"
						)!!.isNotEmpty()
					) {
						content.insert(0, config.getString("discordPingMessage") + " ")
					}

					webhook.setContent(content.toString().trim { it <= ' ' })
				}
			} catch (e: Exception) {
				throwException("Error sending additional pings to Discord: " + e.message)
			} finally {
				try {
					webhook.execute()
				} catch (e: IOException) {
					throwException("Error sending bug report to Discord: " + e.message)
				}
			}
		} else {
			try {
				webhook.execute()
			} catch (e: IOException) {
				throwException("Error sending bug report to Discord: " + e.message)
			}
		}
	}

	private fun throwException(message: String) {
		plugin.logger.warning(message)
		logErrorMessage(message)
	}

	private fun getCategoryName(category: Int): String? {
		val categoryList = config.getMapList("reportCategories")
		for (categoryMap in categoryList) {
			if (categoryMap["id"] == category) {
				return categoryMap["name"] as String?
			}
		}
		return "Unknown Category"
	}

	private fun getUserIDFromAPI(username: String): String {
		val url = "https://playerdb.co/api/player/minecraft/$username"
		val content = StringBuilder()

		try {
			val playerdb = URL(url)
			val connection = playerdb.openConnection() as HttpURLConnection

			connection.requestMethod = "GET"
			connection.setRequestProperty("Content-Type", "application/json")
			connection.setRequestProperty("User-Agent", "BugReport/0.12.3")
			connection.connectTimeout = 5000
			connection.readTimeout = 5000
			connection.doOutput = true

			val `in` = BufferedReader(InputStreamReader(connection.inputStream))
			var inputLine: String?
			while (`in`.readLine().also { inputLine = it } != null) {
				content.append(inputLine)
			}
			`in`.close()
			connection.disconnect()
			errorLogged = false
		} catch (e: Exception) {
			if (!errorLogged) {
				getLogger().warning("Error getting UUID from API: " + e.message)
				logErrorMessage("Error getting UUID from API: " + e.message)
				errorLogged = true
			}
			return "Unknown UUID"
		}

		val splitContent =
			content.toString().split("\"raw_id\":\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

		return if (splitContent.size > 1) {
			splitContent[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
		} else {
			"Unknown UUID"
		}
	}

	companion object {
		private const val EMBED_TITLE = "New Bug Report"
		private const val EMBED_FOOTER_TEXT = "Bug Report V0.12.3"
		private const val EMBED_THUMBNAIL = "https://www.spigotmc.org/data/resource_icons/110/110732.jpg"
		private val EMBED_COLOR: Color = Color.YELLOW
		private var errorLogged = false
	}
}
