package com.bugreportmc.bugreport.extensions

import com.bugreportmc.bugreport.BugReportDatabase
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadArchivedBugReportCountForPlayer
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadBugReportAllPlayer
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadBugReportCountForPlayer
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadBugReportCountsPerPlayer
import com.bugreportmc.bugreport.BugReportDatabase.Companion.loadNonArchivedBugReportCountForPlayer
import com.djrapitops.plan.extension.CallEvents
import com.djrapitops.plan.extension.DataExtension
import com.djrapitops.plan.extension.annotation.NumberProvider
import com.djrapitops.plan.extension.annotation.PluginInfo
import com.djrapitops.plan.extension.annotation.TableProvider
import com.djrapitops.plan.extension.icon.Color
import com.djrapitops.plan.extension.icon.Family
import com.djrapitops.plan.extension.icon.Icon
import com.djrapitops.plan.extension.table.Table
import java.util.*

@PluginInfo(name = "Bug Report Plugin", iconName = "bug", iconFamily = Family.SOLID, color = Color.YELLOW)
class BugReportExtension : DataExtension {
	override fun callExtensionMethodsOn(): Array<CallEvents> {
		return arrayOf(
			CallEvents.PLAYER_JOIN,
			CallEvents.PLAYER_LEAVE,
			CallEvents.SERVER_EXTENSION_REGISTER,
			CallEvents.SERVER_PERIODICAL,
			CallEvents.MANUAL
		)
	}

	@NumberProvider(
		text = "Bug Reports",
		description = "How many bug reports the player has submitted",
		iconName = "bookmark",
		iconColor = Color.YELLOW,
		priority = 100,
		showInPlayerTable = true
	)
	fun bugReportCount(playerUUID: UUID): Long {
		return loadBugReportCountForPlayer(playerUUID)
	}

	@NumberProvider(
		text = "Archived Bug Reports",
		description = "How many archived bug reports the player has submitted",
		iconName = "box-archive",
		iconColor = Color.ORANGE,
		priority = 90,
		showInPlayerTable = true
	)
	fun archivedBugReportCount(playerUUID: UUID): Long {
		return loadArchivedBugReportCountForPlayer(playerUUID)
	}

	@NumberProvider(
		text = "Non-Archived Bug Reports",
		description = "How many non-archived bug reports the player has submitted",
		iconName = "bookmark",
		iconColor = Color.LIGHT_GREEN,
		priority = 80,
		showInPlayerTable = true
	)
	fun nonArchivedBugReportCount(playerUUID: UUID): Long {
		return loadNonArchivedBugReportCountForPlayer(playerUUID)
	}

	@TableProvider(tableColor = Color.AMBER)
	fun bugReportPlayerHistory(playerName: String?): Table {
		val bugReportPlayerTable: Table.Factory =
			Table.builder().columnOne("Date Submitted", Icon(Family.SOLID, "gavel", Color.AMBER))
				.columnTwo("Reported Bug", Icon(Family.SOLID, "bug", Color.AMBER))

		val allBugReports: List<BugReportPair<String, String>> = loadBugReportAllPlayer(playerName)
		for (report in allBugReports) {
			val timestampString = report.first
			val bugMessage = report.second
			bugReportPlayerTable.addRow(timestampString, bugMessage)
		}

		return bugReportPlayerTable.build()
	}

	@TableProvider(tableColor = Color.AMBER)
	fun bugReportCountHistory(): Table {
		val bugReportServer: Table.Factory =
			Table.builder().columnOne("Bug Reporter", Icon(Family.SOLID, "gavel", Color.AMBER))
				.columnTwo("Reported Bug", Icon(Family.SOLID, "bug", Color.AMBER))

		val allBugReports: List<BugReportPair<String, String>> = loadBugReportCountsPerPlayer()
		for (report in allBugReports) {
			val reporter = report.first
			val bugMessage = report.second
			bugReportServer.addRow(reporter, bugMessage)
		}

		return bugReportServer.build()
	}
}
