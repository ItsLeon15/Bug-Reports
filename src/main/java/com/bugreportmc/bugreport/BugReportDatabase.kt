package com.bugreportmc.bugreport

import com.bugreportmc.bugreport.BugReportManager.Companion.bugReports
import com.bugreportmc.bugreport.BugReportManager.Companion.config
import com.bugreportmc.bugreport.BugReportManager.Companion.debugMode
import com.bugreportmc.bugreport.BugReportManager.Companion.loadConfig
import com.bugreportmc.bugreport.BugReportManager.Companion.translateTimestampToDate
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.bugreportmc.bugreport.extensions.BugReportPair
import com.bugreportmc.bugreport.extensions.PlanHook
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Bukkit.getWorld
import org.bukkit.Location
import java.io.File
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class BugReportDatabase {
	init {
		createConnection()

		addColumnIfNotExists("player_data", "player_id", "TEXT, last_login_timestamp BIGINT DEFAULT 0")
		addColumnIfNotExists("bug_reports", "archived", "INTEGER DEFAULT 0")
		addColumnIfNotExists("bug_reports", "report_id", "INT AUTO_INCREMENT PRIMARY KEY")
		addColumnIfNotExists("bug_reports", "location", "TEXT")
		addColumnIfNotExists("bug_reports", "gamemode", "TEXT")
		addColumnIfNotExists("bug_reports", "serverName", "TEXT")

		fixReportID()
		makeAllHeadersEqualReportID()
		addTimestampColumn()
	}

	private fun makeAllHeadersEqualReportID() {
		try {
			dataSource.getConnection().use { connection ->
				val resultSet: ResultSet = connection.createStatement().executeQuery("SELECT * FROM bug_reports")
				while (resultSet.next()) {
					val reportId: Int = resultSet.getInt("report_id")
					val header: String = resultSet.getString("header")
					val lines = header.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					val newHeader = StringBuilder()

					for (line in lines) {
						if (line.startsWith("Report ID:")) {
							newHeader.append("Report ID: ").append(reportId)
						} else {
							newHeader.append(line)
						}
						newHeader.append("\n")
					}

					val statement: PreparedStatement =
						connection.prepareStatement("UPDATE bug_reports SET header = ? WHERE report_id = ?")
					statement.setString(1, newHeader.toString().trim { it <= ' ' })
					statement.setInt(2, reportId)
					statement.executeUpdate()
					statement.close()
				}
			}
		} catch (e: Exception) {
			plugin.logger.severe("Failed to make all headers equal report_id.")
			plugin.logger.severe(e.message)
		}
	}

	private fun fixReportID() {
		try {
			dataSource.getConnection().use { connection ->
				val resultSet: ResultSet = connection.createStatement()
					.executeQuery("SELECT * FROM bug_reports WHERE report_id IS NULL OR report_id = 0")
				while (resultSet.next()) {
					val reportId: Int = resultSet.getInt("report_id")
					val rowNumber: Int = resultSet.row
					if (reportId != rowNumber) {
						val statement: PreparedStatement =
							connection.prepareStatement("UPDATE bug_reports SET report_id = ? WHERE report_id = ?")
						statement.setInt(1, rowNumber)
						statement.setInt(2, reportId)
						statement.executeUpdate()
						statement.close()
					}
				}
			}
		} catch (e: Exception) {
			plugin.logger.severe("Failed to fix report_id.")
			plugin.logger.severe(e.message)
		}
	}

	fun addBugReport(
		username: String?,
		playerId: UUID,
		world: String?,
		header: String?,
		fullMessage: String?,
		location: String?,
		gamemode: String?,
		serverName: String?,
	) {
		try {
			dataSource.getConnection().use { connection ->
				val statement: PreparedStatement =
					connection.prepareStatement("INSERT INTO bug_reports(player_id, header, message, username, world, archived, report_id, timestamp, location, gamemode, status, serverName) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
				var reportId = 1
				val resultSet: ResultSet = connection.createStatement()
					.executeQuery("SELECT report_id FROM bug_reports ORDER BY report_id DESC LIMIT 1")
				if (resultSet.next()) {
					reportId = resultSet.getInt("report_id") + 1
				}
				statement.setString(1, playerId.toString())
				statement.setString(2, header)
				statement.setString(3, fullMessage)
				statement.setString(4, username)
				statement.setString(5, world)
				statement.setInt(6, 0)
				statement.setInt(7, reportId)
				statement.setLong(8, System.currentTimeMillis())
				statement.setString(9, location)
				statement.setString(10, gamemode)
				statement.setString(11, "0")
				statement.setString(12, serverName)

				if (getPluginManager().isPluginEnabled("Plan")) {
					PlanHook.instance!!.updateHook(playerId, username)
				}

				statement.executeUpdate()
				statement.close()
			}
		} catch (e: Exception) {
			plugin.logger.severe("Failed to add bug report.")
			plugin.logger.severe(e.message)
		}
	}

	companion object {
		lateinit var dataSource: HikariDataSource

		@JvmStatic
		fun reloadConnection() {
			dataSource.close()
			createConnection()
		}

		private fun addTimestampColumn() {
			try {
				dataSource.connection.use { connection ->
					connection.metaData.getColumns(null, null, "player_data", "last_login_timestamp")
						.use { archivedResultSet ->
							if (!archivedResultSet.next()) {
								connection.createStatement()
									.execute("ALTER TABLE player_data ADD COLUMN last_login_timestamp BIGINT DEFAULT 0")
							}
						}
					connection.metaData.getColumns(null, null, "bug_reports", "timestamp").use { archivedResultSet ->
						if (!archivedResultSet.next()) {
							connection.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN timestamp BIGINT")
						}
					}
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to add missing columns.")
				plugin.logger.severe(e.message)
				logErrorMessage("Failed to add missing columns." + e.message)
			}
		}

		fun setPlayerLastLoginTimestamp(playerId: UUID) {
			try {
				dataSource.connection.use { connection ->
					if (getPlayerLastLoginTimestamp(playerId) == 0L) {
						val playerDataStatement: PreparedStatement =
							connection.prepareStatement("INSERT INTO player_data(player_id, last_login_timestamp) VALUES(?, ?)")
						playerDataStatement.setString(1, playerId.toString())
						playerDataStatement.setLong(2, System.currentTimeMillis())
						playerDataStatement.executeUpdate()
						playerDataStatement.close()
					} else {
						val playerDataStatement: PreparedStatement =
							connection.prepareStatement("UPDATE player_data SET last_login_timestamp = ? WHERE player_id = ?")
						playerDataStatement.setLong(1, System.currentTimeMillis())
						playerDataStatement.setString(2, playerId.toString())
						playerDataStatement.executeUpdate()
						playerDataStatement.close()
					}
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to set player last login timestamp.")
				plugin.logger.severe(e.message)
				logErrorMessage("Failed to set player last login timestamp." + e.message)
			}
		}

		fun getPlayerLastLoginTimestamp(playerId: UUID): Long {
			try {
				dataSource.connection.use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT last_login_timestamp FROM player_data WHERE player_id = ?")
					statement.setString(1, playerId.toString())
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						return resultSet.getLong("last_login_timestamp")
					}
					statement.close()
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to get player last login timestamp.")
				plugin.logger.severe(e.message)
				logErrorMessage("Failed to get player last login timestamp." + e.message)
			}
			return 0
		}

		fun getBugReportLocation(reportIDGUI: Int): Location? {
			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT location FROM bug_reports WHERE report_id = ?")
					statement.setInt(1, reportIDGUI)
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						val locationString: String = resultSet.getString("Location")
						val locationSplit =
							locationString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
						return Location(
							getWorld(locationSplit[0]),
							locationSplit[1].toDouble(),
							locationSplit[2].toDouble(),
							locationSplit[3].toDouble()
						)
					}
					statement.close()
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to get bug report location.")
				plugin.logger.severe(e.message)
				logErrorMessage("Failed to get bug report location." + e.message)
			}
			return null
		}

		private fun addColumnIfNotExists(tableName: String, columnName: String, columnDefinition: String) {
			try {
				dataSource.getConnection().use { connection ->
					connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
						if (!resultSet.next()) {
							val query = String.format(
								"ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition
							)
							connection.createStatement().execute(query)
						}
					}
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to add missing columns.")
				plugin.logger.severe(e.message)
				logErrorMessage("Failed to add missing columns." + e.message)
			}
		}

		fun createConnection() {
			loadConfig()
			val databaseType = config.getString("databaseType")
			val databaseSection = config.getConfigurationSection("database")

			if (databaseType.equals("local", ignoreCase = true)) {
				plugin.logger.info("Connecting to local database")
				connectLocal()
			} else if (databaseType.equals("mysql", ignoreCase = true)) {
				plugin.logger.info("Connecting to remote database")

				databaseSection!!
				val host: String = databaseSection.getString("host").toString()
				val port: Int = databaseSection.getInt("port")
				val database: String = databaseSection.getString("database").toString()
				val username: String = databaseSection.getString("username").toString()
				val password: String = databaseSection.getString("password").toString()

				connectRemote(host, port, database, username, password)
			} else {
				plugin.logger.warning("Invalid database type. Please use 'local' or 'mysql'.")
				logErrorMessage("Invalid database type. Please use 'local' or 'mysql'.")
			}
		}

		@JvmStatic
		fun loadDeletedBugReportCount(): Long {
			try {
				dataSource.getConnection().use { connection ->
					connection.prepareStatement("SELECT total_deleted FROM bugreport_analytics").use { statement ->
						statement.executeQuery().use { resultSet ->
							if (resultSet.next()) {
								return resultSet.getLong("total_deleted")
							}
						}
					}
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}
			return 0
		}

		fun loadBugReportCountForPlayer(playerID: UUID): Long {
			var count = 0

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE player_id = ?")
					statement.setString(1, playerID.toString())
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						count = resultSet.getInt(1)
					}
					resultSet.close()
					statement.close()
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}

			return count.toLong()
		}

		fun loadArchivedBugReportCountForPlayer(playerID: UUID): Long {
			var count = 0

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE player_id = ? AND archived = 1")
					statement.setString(1, playerID.toString())
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						count = resultSet.getInt(1)
					}
					resultSet.close()
					statement.close()
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}

			return count.toLong()
		}

		fun loadNonArchivedBugReportCountForPlayer(playerID: UUID): Long {
			var count = 0

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE player_id = ? AND archived = 0")
					statement.setString(1, playerID.toString())
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						count = resultSet.getInt(1)
					}
					resultSet.close()
					statement.close()
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}

			return count.toLong()
		}

		@JvmStatic
		fun loadBugReportCount(): Long {
			var count = 0

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports")
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						count = resultSet.getInt(1)
					}
					resultSet.close()
					statement.close()
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}

			return count.toLong()
		}

		@JvmStatic
		fun loadBugReportCountsPerPlayer(): List<BugReportPair<String, String>> {
			val reports: MutableList<BugReportPair<String, String>> = ArrayList<BugReportPair<String, String>>()
			try {
				dataSource.getConnection().use { connection ->
					val sql = "SELECT username, message FROM bug_reports"
					connection.prepareStatement(sql).use { statement ->
						statement.executeQuery().use { resultSet ->
							while (resultSet.next()) {
								val username: String = resultSet.getString("username")
								val message: String = resultSet.getString("message")
								reports.add(BugReportPair(username, message))
							}
						}
					}
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}
			return reports
		}

		fun loadBugReportAllPlayer(playerName: String?): List<BugReportPair<String, String>> {
			val reports: MutableList<BugReportPair<String, String>> = ArrayList<BugReportPair<String, String>>()
			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT timestamp, message FROM bug_reports WHERE username = ?")
					statement.setString(1, playerName)
					val resultSet: ResultSet = statement.executeQuery()
					while (resultSet.next()) {
						val timestamp: Long = resultSet.getLong("timestamp")
						val message: String = resultSet.getString("message")
						val timestampToString: String = translateTimestampToDate(timestamp)
						reports.add(BugReportPair(timestampToString, message))
					}
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}
			return reports
		}

		@JvmStatic
		fun loadArchivedBugReportCount(): Long {
			var count = 0

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE archived = 1")
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						count = resultSet.getInt(1)
						resultSet.close()
						statement.close()
					}
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}

			return count.toLong()
		}

		@JvmStatic
		fun loadNonArchivedBugReportCount(): Long {
			var count = 0

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE archived = 0")
					val resultSet: ResultSet = statement.executeQuery()
					if (resultSet.next()) {
						count = resultSet.getInt(1)
						resultSet.close()
						statement.close()
					}
				}
			} catch (e: SQLException) {
				plugin.logger.severe(e.message)
				logErrorMessage(e.message.toString())
			}

			return count.toLong()
		}

		fun loadBugReports(): Map<UUID, ArrayList<String>> {
			var bugReports: Map<UUID, ArrayList<String>> = HashMap<UUID, ArrayList<String>>()
			if (debugMode) {
				plugin.logger.info("Loading bug reports")
			}

			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("SELECT * FROM bug_reports ORDER BY report_id ASC")
					val resultSet: ResultSet = statement.executeQuery()

					while (resultSet.next()) {
						val playerId: UUID = UUID.fromString(resultSet.getString("player_id"))
						val header: String = resultSet.getString("header")
						val fullMessage: String = resultSet.getString("message")
						val username: String = resultSet.getString("username")
						val world: String = resultSet.getString("world")
						val archived: String = resultSet.getString("archived")
						val reportId: String = resultSet.getString("report_id")
						val timestamp: Long = resultSet.getLong("timestamp")
						val location: String = resultSet.getString("location")
						val gamemode: String = resultSet.getString("gamemode")
						val serverName: String = resultSet.getString("serverName")
						val status: String = resultSet.getString("status")

						val reports = bugReports.getOrDefault(
							getStaticUUID(), ArrayList(listOf("DUMMY"))
						)
						reports.add("Username: $username\nUUID: $playerId\nWorld: $world\nFull Message: $fullMessage\nHeader: $header\nArchived: $archived\nReport ID: $reportId\nTimestamp: $timestamp\nLocation: $location\nGamemode: $gamemode\nStatus: $status\nServer Name: $serverName")

						if (getPluginManager().isPluginEnabled("Plan")) {
							PlanHook.instance?.updateHook(playerId, username)
						}

						bugReports = bugReports.toMutableMap().apply { put(getStaticUUID(), reports) }
					}

					if (debugMode) {
						plugin.logger.info("Loaded " + bugReports.size + " bug reports")
					}

					resultSet.close()
					statement.close()
				}
			} catch (e: SQLException) {
				plugin.logger.severe("Failed to load bug reports.")
				logErrorMessage("Failed to load bug reports.")
				if (e.message.toString().startsWith("[SQLITE_CORRUPT]")) {
					plugin.logger.severe("Your database is corrupted. Please delete the database file and restart the server.")
					plugin.logger.severe("File path: plugins/BugReport/bugreports.db")
					plugin.logger.severe("If you need help, please join the discord server: https://discord.gg/ZvdNYqmsbx")
				} else {
					plugin.logger.severe(e.message)
				}
			}

			return bugReports
		}

		private fun connectRemote(host: String, port: Int, database: String, username: String, password: String) {
			val hikariConfig = HikariConfig()
			try {
				hikariConfig.setJdbcUrl("jdbc:mysql://$host:$port/$database?useSSL=false")
				hikariConfig.username = username
				hikariConfig.password = password
				dataSource = HikariDataSource(hikariConfig)
				plugin.logger.info("Connected to remote database")
			} catch (e: Exception) {
				plugin.logger.severe("Failed to connect to remote database.")
				plugin.logger.severe(e.message)
			}

			createTables()
		}

		private fun connectLocal() {
			try {
				val databaseFile = File("plugins/BugReport/bugreports.db")
				val hikariConfig = HikariConfig()
				hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.absolutePath)
				dataSource = HikariDataSource(hikariConfig)
				plugin.logger.info("Connected to local database")
			} catch (e: Exception) {
				plugin.logger.severe("Failed to connect to local database.")
				plugin.logger.severe(e.message)
			}

			createTables()
		}

		private fun createTables() {
			try {
				dataSource.getConnection().use { connection ->
					connection.createStatement()
						.execute("CREATE TABLE IF NOT EXISTS bug_reports(rowid INTEGER, player_id TEXT, header TEXT, message TEXT, username TEXT, world TEXT, archived INTEGER DEFAULT 0, report_id INTEGER, timestamp BIGINT, status TEXT, serverName TEXT)")
					connection.createStatement()
						.execute("CREATE TABLE IF NOT EXISTS player_data(player_id TEXT, last_login_timestamp BIGINT DEFAULT 0)")
					connection.createStatement()
						.execute("CREATE TABLE IF NOT EXISTS bugreport_analytics(total_deleted INTEGER DEFAULT 0)")
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to create tables.")
				plugin.logger.severe(e.message)
			}
		}

		fun updateReportStatus(reportIDGUI: Int, statusID: Int) {
			if (debugMode) {
				plugin.logger.info("Updating report status for report ID $reportIDGUI")
			}
			try {
				dataSource.getConnection().use { connection ->
					connection.prepareStatement("UPDATE bug_reports SET status = ? WHERE report_id = ?")
						.use { statement ->
							statement.setInt(1, statusID)
							statement.setInt(2, reportIDGUI)
							statement.executeUpdate()
						}
					val reports: ArrayList<String> = bugReports.getOrDefault(
						getStaticUUID(), ArrayList<String>(listOf("DUMMY"))
					)

					val existingHeader = reports.stream().filter { reportString: String? ->
						reportString!!.contains(
							"Report ID: $reportIDGUI"
						)
					}.findFirst().orElse(null)
					val existingHeaderPosition = reports.indexOf(existingHeader)
					val lines: Array<String?> =
						existingHeader?.split("\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
							?: arrayOfNulls(0)
					val newHeader = StringBuilder()
					for (line in lines) {
						if (line?.startsWith("Status:") == true) {
							newHeader.append("Status: ").append(statusID)
						} else {
							newHeader.append(line)
						}
						newHeader.append("\n")
					}
					reports[existingHeaderPosition] = newHeader.toString().trim { it <= ' ' }
					bugReports = bugReports.toMutableMap().apply { put(getStaticUUID(), reports) }
					if (debugMode) {
						plugin.logger.info("Updated report status for report ID $reportIDGUI")
					}
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to update bug report status.")
				plugin.logger.severe(e.message)
			}
		}

		fun updateBugReportArchive(reportIndex: Int, archived: Int) {
			if (debugMode) {
				plugin.logger.info("Updating bug report archive status for report ID $reportIndex")
			}
			try {
				dataSource.getConnection().use { connection ->
					val statement: PreparedStatement =
						connection.prepareStatement("UPDATE bug_reports SET archived = ? WHERE report_id = ?")
					statement.setInt(1, archived)
					statement.setInt(2, reportIndex)
					statement.executeUpdate()
					statement.close()
					loadBugReports()

					val reports: ArrayList<String> = bugReports.getOrDefault(
						getStaticUUID(), ArrayList<String>(listOf("DUMMY"))
					)
					val existingHeader = reports.stream().filter { reportString: String? ->
						reportString!!.contains(
							"Report ID: $reportIndex"
						)
					}.findFirst().orElse(null)
					val existingHeaderPosition = reports.indexOf(existingHeader)

					val lines: Array<String?> =
						existingHeader.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					val newHeader = StringBuilder()
					for (line in lines) {
						if (line != null) {
							if (line.startsWith("Archived:")) {
								newHeader.append("Archived: ").append(archived)
							} else {
								newHeader.append(line)
							}
						}
						newHeader.append("\n")
					}
					reports[existingHeaderPosition] = newHeader.toString().trim { it <= ' ' }
					bugReports = bugReports.toMutableMap().apply { put(getStaticUUID(), reports) }
					if (debugMode) {
						plugin.logger.info("Updated bug report archive status for report ID $reportIndex")
					}
				}
			} catch (e: Exception) {
				plugin.logger.severe("Failed to update bug report archive status.")
				plugin.logger.severe(e.message)
			}
		}

		fun deleteBugReport(reportIndex: Int) {
			if (debugMode) {
				plugin.logger.info("Deleting bug report for report ID $reportIndex")
			}
			try {
				dataSource.getConnection().use { connection ->
					connection.prepareStatement("DELETE FROM bug_reports WHERE report_id = ?").use { statement ->
						statement.setInt(1, reportIndex)
						val rowsAffected: Int = statement.executeUpdate()
						if (debugMode) {
							plugin.logger.info("Deleted bug report rows affected: $rowsAffected")
						}
					}
					var totalDeleted = 0
					connection.prepareStatement("SELECT total_deleted FROM bugreport_analytics")
						.use { bugreportAnalytics ->
							bugreportAnalytics.executeQuery().use { resultSet ->
								if (resultSet.next()) {
									totalDeleted = resultSet.getInt("total_deleted")
								}
							}
						}
					if (totalDeleted > 0) {
						connection.prepareStatement("UPDATE bugreport_analytics SET total_deleted = ?")
							.use { analyticsStatement ->
								analyticsStatement.setInt(1, totalDeleted + 1)
								val updateRowsAffected: Int = analyticsStatement.executeUpdate()
								if (debugMode) {
									plugin.logger.info("Updated total_deleted, rows affected: $updateRowsAffected")
									plugin.logger.info("Updated bug report total deleted to " + (totalDeleted + 1))
								}
							}
					} else {
						connection.prepareStatement("INSERT INTO bugreport_analytics (total_deleted) VALUES (?)")
							.use { insertStatement ->
								insertStatement.setInt(1, 1)
								val insertRowsAffected: Int = insertStatement.executeUpdate()
								if (debugMode) {
									plugin.logger.info("Inserted initial bug report total_deleted value of 1, rows affected: $insertRowsAffected")
								}
							}
					}
				}
			} catch (e: SQLException) {
				plugin.logger.severe("Failed to delete bug report or update analytics.")
				plugin.logger.severe(e.message)
			}

			loadBugReports()
		}

		fun getStaticUUID(): UUID {
			return UUID.fromString("00000000-0000-0000-0000-000000000000")
		}
	}
}
