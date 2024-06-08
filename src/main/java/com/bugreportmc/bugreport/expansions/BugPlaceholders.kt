package com.bugreportmc.bugreport.expansions

import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadArchivedBugReportCount
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadBugReportCount
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadDeletedBugReportCount
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadNonArchivedBugReportCount
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

class BugPlaceholders(plugin: Plugin) : PlaceholderExpansion() {
	override fun persist(): Boolean {
		return true
	}

	override fun canRegister(): Boolean {
		return true
	}

	private val author: String = plugin.description.authors.first()
	private val version: String = plugin.description.version
	private val identifier: String = "bugreports"

	override fun onRequest(player: OfflinePlayer?, params: String): String? {
		return when (params) {
			"totalBugReports" -> {
				loadBugReportCount().toString()
			}

			"totalArchivedBugReports" -> {
				loadArchivedBugReportCount().toString()
			}

			"totalNonArchivedBugReports" -> {
				loadNonArchivedBugReportCount().toString()
			}

			"totalDeletedBugReports" -> {
				loadDeletedBugReportCount().toString()
			}

			else -> {
				null
			}
		}
	}

	/**
	 * The placeholder identifier of this expansion. May not contain %,
	 * {} or _
	 *
	 * @return placeholder identifier that is associated with this expansion
	 */
	override fun getIdentifier(): String {
		return identifier
	}

	/**
	 * The author of this expansion
	 *
	 * @return name of the author for this expansion
	 */
	override fun getAuthor(): String {
		return author
	}

	/**
	 * The version of this expansion
	 *
	 * @return current version of this expansion
	 */
	override fun getVersion(): String {
		return version
	}
}
