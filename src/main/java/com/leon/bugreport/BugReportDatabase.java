package com.leon.bugreport;

import com.leon.bugreport.extensions.BugReportPair;
import com.leon.bugreport.extensions.PlanHook;
import com.leon.bugreport.logging.ErrorMessages;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportManager.*;

public class BugReportDatabase {
	public static HikariDataSource dataSource;

	public BugReportDatabase() {
		createConnection();

		addColumnIfNotExists("player_data", "player_id", "TEXT, last_login_timestamp BIGINT DEFAULT 0");
		addColumnIfNotExists("bug_reports", "archived", "INTEGER DEFAULT 0");
		addColumnIfNotExists("bug_reports", "report_id", "INT AUTO_INCREMENT PRIMARY KEY");
		addColumnIfNotExists("bug_reports", "location", "TEXT");
		addColumnIfNotExists("bug_reports", "gamemode", "TEXT");
		addColumnIfNotExists("bug_reports", "serverName", "TEXT");

		fixReportID();
		makeAllHeadersEqualReport_ID();
		addTimestampColumn();
	}

	public static void reloadConnection() {
		if (dataSource != null) {
			dataSource.close();
		}
		createConnection();
	}

	private static void addTimestampColumn() {
		try (Connection connection = dataSource.getConnection()) {
			try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "player_data", "last_login_timestamp")) {
				if (!archivedResultSet.next()) {
					connection.createStatement().execute("ALTER TABLE player_data ADD COLUMN last_login_timestamp BIGINT DEFAULT 0");
				}
			}
			try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "bug_reports", "timestamp")) {
				if (!archivedResultSet.next()) {
					connection.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN timestamp BIGINT");
				}
			}
			try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "bug_reports", "discordWebhookMessageID")) {
				if (!archivedResultSet.next()) {
					connection.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN discordWebhookMessageID TEXT");
				}
			}
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(35, e.getMessage());

			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static void setPlayerLastLoginTimestamp(UUID playerId) {
		try (Connection connection = dataSource.getConnection()) {
			if (getPlayerLastLoginTimestamp(playerId) == 0) {
				PreparedStatement playerDataStatement = connection.prepareStatement("INSERT INTO player_data(player_id, last_login_timestamp) VALUES(?, ?)");
				playerDataStatement.setString(1, playerId.toString());
				playerDataStatement.setLong(2, System.currentTimeMillis());
				playerDataStatement.executeUpdate();
				playerDataStatement.close();
			} else {
				PreparedStatement playerDataStatement = connection.prepareStatement("UPDATE player_data SET last_login_timestamp = ? WHERE player_id = ?");
				playerDataStatement.setLong(1, System.currentTimeMillis());
				playerDataStatement.setString(2, playerId.toString());
				playerDataStatement.executeUpdate();
				playerDataStatement.close();
			}
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(36, e.getMessage());

			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static long getPlayerLastLoginTimestamp(UUID playerId) {
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT last_login_timestamp FROM player_data WHERE player_id = ?");
			statement.setString(1, playerId.toString());
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getLong("last_login_timestamp");
			}
			statement.close();
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(37, e.getMessage());

			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
		}
		return 0;
	}

	private static @NotNull String getField(@NotNull String input, String fieldName) {
		String[] lines = input.split("\n");

		String prefix = fieldName + ": ";
		for (String line : lines) {
			if (line.startsWith(prefix)) {
				return line.substring(prefix.length());
			}
		}

		return "Unknown";
	}

	public static @NotNull Map<String, String> getBugReportById(int reportId) {
		Map<String, String> allData = new HashMap<>();

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM bug_reports WHERE report_id = ?");
			statement.setInt(1, reportId);
			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next()) {
				String Username = resultSet.getString("username");
				String UUID = resultSet.getString("player_id");
				String World = resultSet.getString("world");
				String FullMessage = resultSet.getString("message");

				String Header = resultSet.getString("header");
				String CategoryID = getField(Header, "Category ID");

				String Location = resultSet.getString("location");
				String Gamemode = resultSet.getString("gamemode");
				String Status = resultSet.getString("status");
				String ServerName = resultSet.getString("serverName");

				allData.put("Username", Username);
				allData.put("UUID", UUID);
				allData.put("World", World);
				allData.put("FullMessage", FullMessage);
				allData.put("Header", Header);
				allData.put("CategoryID", CategoryID);
				allData.put("Location", Location);
				allData.put("Gamemode", Gamemode);
				allData.put("Status", Status);
				allData.put("ServerName", ServerName);
			}

			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to get bug report by ID.");
			plugin.getLogger().severe(e.getMessage());
		}

		return allData;
	}

	public static @NotNull String getBugReportById(int reportId, boolean isArchived) {
		StringBuilder reportBuilder = new StringBuilder();

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM bug_reports WHERE report_id = ? AND archived = ?");
			statement.setInt(1, reportId);
			statement.setInt(2, isArchived ? 1 : 0);

			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next()) {
				String username = resultSet.getString("username");
				String playerId = resultSet.getString("player_id");
				String world = resultSet.getString("world");
				String fullMessage = resultSet.getString("message");
				String header = resultSet.getString("header");
				String archived = resultSet.getString("archived");
				String report_id = resultSet.getString("report_id");
				long timestamp = resultSet.getLong("timestamp");
				String location = resultSet.getString("location");
				String gamemode = resultSet.getString("gamemode");
				String serverName = resultSet.getString("serverName");
				String status = resultSet.getString("status");

				reportBuilder
						.append("Username: ").append(username).append("\n")
						.append("UUID: ").append(playerId).append("\n")
						.append("World: ").append(world).append("\n")
						.append("Full Message: ").append(fullMessage).append("\n")
						.append("Header: ").append(header).append("\n")
						.append("Archived: ").append(archived).append("\n")
						.append("Report ID: ").append(report_id).append("\n")
						.append("Timestamp: ").append(timestamp).append("\n")
						.append("Location: ").append(location).append("\n")
						.append("Gamemode: ").append(gamemode).append("\n")
						.append("Status: ").append(status).append("\n")
						.append("Server Name: ").append(serverName);
			}

			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to get bug report by ID.");
			plugin.getLogger().severe(e.getMessage());
		}

		return reportBuilder.toString();
	}

	public static @Nullable Location getBugReportLocation(Integer reportIDGUI) {
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT location FROM bug_reports WHERE report_id = ?");
			statement.setInt(1, reportIDGUI);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				String locationString = resultSet.getString("Location");
				if (locationString != null) {
					String[] locationSplit = locationString.split(",");
					return new Location(Bukkit.getWorld(locationSplit[0]), Double.parseDouble(locationSplit[1]), Double.parseDouble(locationSplit[2]), Double.parseDouble(locationSplit[3]));
				}
			}
			statement.close();
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(38, e.getMessage());

			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
		}
		return null;
	}

	private static void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
		try (Connection connection = dataSource.getConnection()) {
			try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
				if (!resultSet.next()) {
					String query = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
					connection.createStatement().execute(query);
				}
			}
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(39, e.getMessage());

			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static void createConnection() {
		loadConfig();
		String databaseType = Objects.requireNonNull(config.getString("databaseType"));
		ConfigurationSection databaseSection = Objects.requireNonNull(config.getConfigurationSection("database"));

		if (databaseType.equalsIgnoreCase("local")) {
			plugin.getLogger().info("Connecting to local database");
			connectLocal();
		} else if (databaseType.equalsIgnoreCase("mysql")) {
			plugin.getLogger().info("Connecting to remote database");

			String host = databaseSection.getString("host");
			int port = databaseSection.getInt("port");
			String database = databaseSection.getString("database");
			String username = databaseSection.getString("username");
			String password = databaseSection.getString("password");

			connectRemote(host, port, database, username, password);
		} else {
			String errorMessage = ErrorMessages.getErrorMessage(15);

			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static long loadDeletedBugReportCount() {
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement("SELECT total_deleted FROM bugreport_analytics");
			 ResultSet resultSet = statement.executeQuery()) {

			if (resultSet.next()) {
				return resultSet.getLong("total_deleted");
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}
		return 0;
	}

	public static long loadBugReportCountForPlayer(@NotNull UUID playerID) {
		int count = 0;

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE player_id = ?");
			statement.setString(1, playerID.toString());
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}
			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}

		return count;
	}

	public static long loadArchivedBugReportCountForPlayer(@NotNull UUID playerID) {
		int count = 0;

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE player_id = ? AND archived = 1");
			statement.setString(1, playerID.toString());
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}
			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}

		return count;
	}

	public static long loadNonArchivedBugReportCountForPlayer(@NotNull UUID playerID) {
		int count = 0;

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE player_id = ? AND archived = 0");
			statement.setString(1, playerID.toString());
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}
			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}

		return count;
	}

	public static long loadBugReportCount() {
		int count = 0;

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports");
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}
			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}

		return count;
	}

	public static @NotNull List<BugReportPair<String, String>> loadBugReportCountsPerPlayer() {
		List<BugReportPair<String, String>> reports = new ArrayList<>();
		try (Connection connection = dataSource.getConnection()) {
			String sql = "SELECT username, message FROM bug_reports";
			try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					String username = resultSet.getString("username");
					String message = resultSet.getString("message");
					reports.add(new BugReportPair<>(username, message));
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}
		return reports;
	}

	public static @NotNull List<BugReportPair<String, String>> loadBugReportAllPlayer(String playerName) {
		List<BugReportPair<String, String>> reports = new ArrayList<>();
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT timestamp, message FROM bug_reports WHERE username = ?");
			statement.setString(1, playerName);
			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				long timestamp = resultSet.getLong("timestamp");
				String message = resultSet.getString("message");
				String timestampToString = translateTimestampToDate(timestamp);
				reports.add(new BugReportPair<>(timestampToString, message));
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}
		return reports;
	}

	public static long loadArchivedBugReportCount() {
		int count = 0;

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE archived = 1");
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				count = resultSet.getInt(1);
				resultSet.close();
				statement.close();
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}

		return count;
	}

	public static long loadNonArchivedBugReportCount() {
		int count = 0;

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE archived = 0");
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				count = resultSet.getInt(1);
				resultSet.close();
				statement.close();
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
			logErrorMessage(e.getMessage());
		}

		return count;
	}

	public static @NotNull Map<UUID, List<String>> loadBugReports() {
		Map<UUID, List<String>> bugReports = new HashMap<>();
		if (debugMode) {
			plugin.getLogger().info("Loading bug reports");
		}

		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM bug_reports ORDER BY report_id ASC");
			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				UUID playerId = UUID.fromString(resultSet.getString("player_id"));
				String header = resultSet.getString("header");
				String fullMessage = resultSet.getString("message");
				String username = resultSet.getString("username");
				String world = resultSet.getString("world");
				String archived = resultSet.getString("archived");
				String report_id = resultSet.getString("report_id");
				long timestamp = resultSet.getLong("timestamp");
				String location = resultSet.getString("location");
				String gamemode = resultSet.getString("gamemode");
				String serverName = resultSet.getString("serverName");
				String status = resultSet.getString("status");

				List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
				reports.add("Username: " + username + "\n" + "UUID: " + playerId + "\n" + "World: " + world + "\n" + "Full Message: " + fullMessage + "\n" + "Header: " + header + "\n" + "Archived: " + archived + "\n" + "Report ID: " + report_id + "\n" + "Timestamp: " + timestamp + "\n" + "Location: " + location + "\n" + "Gamemode: " + gamemode + "\n" + "Status: " + status + "\n" + "Server Name: " + serverName);

				if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {
					PlanHook.getInstance().updateHook(playerId, username);
				}

				bugReports.put(getStaticUUID(), reports);
			}

			if (debugMode) {
				plugin.getLogger().info("Loaded " + bugReports.size() + " bug reports");
			}

			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(40, e.getMessage());

			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
			if (e.getMessage().startsWith("[SQLITE_CORRUPT]")) {
				String errorMessageCorrupt = ErrorMessages.getErrorMessage(41);

				plugin.getLogger().severe(errorMessageCorrupt);
				plugin.getLogger().severe("If you need help, please join the discord server: https://discord.gg/ZvdNYqmsbx");
			} else {
				plugin.getLogger().severe(e.getMessage());
			}
		}

		return bugReports;
	}

	public static @NotNull UUID getStaticUUID() {
		return UUID.fromString("00000000-0000-0000-0000-000000000000");
	}

	private static void connectRemote(String host, Integer port, String database, String username, String password) {
		HikariConfig hikariConfig = new HikariConfig();
		try {
			hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
			hikariConfig.setUsername(username);
			hikariConfig.setPassword(password);
			dataSource = new HikariDataSource(hikariConfig);
			plugin.getLogger().info("Connected to remote database");
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to connect to remote database.");
			plugin.getLogger().severe(e.getMessage());
		}

		createTables();
	}

	private static void connectLocal() {
		try {
			File databaseFile = new File("plugins/BugReport/bugreports.db");
			if (!databaseFile.exists()) {
				if (databaseFile.createNewFile()) {
					plugin.getLogger().info("Created local database file");
				}
			}
			HikariConfig hikariConfig = new HikariConfig();
			hikariConfig.setDriverClassName("org.sqlite.JDBC");
			hikariConfig.setConnectionTestQuery("SELECT 1");
			hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
			dataSource = new HikariDataSource(hikariConfig);
			plugin.getLogger().info("Connected to local database");
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to connect to local database.");
			plugin.getLogger().severe(e.getMessage());
		}

		createTables();
	}

	private static void createTables() {
		try (Connection connection = dataSource.getConnection()) {
			connection.createStatement().execute("CREATE TABLE IF NOT EXISTS bug_reports(rowid INTEGER, player_id TEXT, header TEXT, message TEXT, username TEXT, world TEXT, archived INTEGER DEFAULT 0, report_id INTEGER, timestamp BIGINT, status TEXT, serverName TEXT, discordWebhookMessageID TEXT)");
			connection.createStatement().execute("CREATE TABLE IF NOT EXISTS player_data(player_id TEXT, last_login_timestamp BIGINT DEFAULT 0)");
			connection.createStatement().execute("CREATE TABLE IF NOT EXISTS bugreport_analytics(total_deleted INTEGER DEFAULT 0)");
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to create tables.");
			plugin.getLogger().severe(e.getMessage());
		}
	}

	public static void updateReportStatus(int reportIDGUI, int statusID) {
		if (debugMode) {
			plugin.getLogger().info("Updating report status for report ID " + reportIDGUI);
		}
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET status = ? WHERE report_id = ?")) {
				statement.setInt(1, statusID);
				statement.setInt(2, reportIDGUI);
				statement.executeUpdate();
			}

			List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));

			String existingHeader = reports.stream().filter(reportString -> reportString.contains("Report ID: " + reportIDGUI)).findFirst().orElse(null);
			int existingHeaderPosition = reports.indexOf(existingHeader);
			String[] lines = existingHeader != null ? existingHeader.split("\n") : new String[0];
			StringBuilder newHeader = new StringBuilder();
			for (String line : lines) {
				if (line.startsWith("Status:")) {
					newHeader.append("Status: ").append(statusID);
				} else {
					newHeader.append(line);
				}
				newHeader.append("\n");
			}
			reports.set(existingHeaderPosition, newHeader.toString().trim());
			bugReports.put(getStaticUUID(), reports);
			if (debugMode) {
				plugin.getLogger().info("Updated report status for report ID " + reportIDGUI);
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to update bug report status.");
			plugin.getLogger().severe(e.getMessage());
		}
	}

	public static void updateBugReportArchive(int reportIndex, int archived) {
		if (debugMode) {
			plugin.getLogger().info("Updating bug report archive status for report ID " + reportIndex);
		}
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET archived = ? WHERE report_id = ?");
			statement.setInt(1, archived);
			statement.setInt(2, reportIndex);
			statement.executeUpdate();
			statement.close();
			loadBugReports();

			List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
			String existingHeader = reports.stream().filter(reportString -> reportString.contains("Report ID: " + reportIndex)).findFirst().orElse(null);
			int existingHeaderPosition = reports.indexOf(existingHeader);

			String[] lines = existingHeader != null ? existingHeader.split("\n") : new String[0];
			StringBuilder newHeader = new StringBuilder();
			for (String line : lines) {
				if (line.startsWith("Archived:")) {
					newHeader.append("Archived: ").append(archived);
				} else {
					newHeader.append(line);
				}
				newHeader.append("\n");
			}
			reports.set(existingHeaderPosition, newHeader.toString().trim());
			bugReports.put(getStaticUUID(), reports);
			if (debugMode) {
				plugin.getLogger().info("Updated bug report archive status for report ID " + reportIndex);
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to update bug report archive status.");
			plugin.getLogger().severe(e.getMessage());
		}
	}

	public static void deleteBugReport(int reportIndex) {
		if (debugMode) {
			plugin.getLogger().info("Deleting bug report for report ID " + reportIndex);
		}
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("DELETE FROM bug_reports WHERE report_id = ?")) {
				statement.setInt(1, reportIndex);
				int rowsAffected = statement.executeUpdate();
				if (debugMode) {
					plugin.getLogger().info("Deleted bug report rows affected: " + rowsAffected);
				}
			}

			int totalDeleted = 0;
			try (PreparedStatement bugreportAnalytics = connection.prepareStatement("SELECT total_deleted FROM bugreport_analytics");
				 ResultSet resultSet = bugreportAnalytics.executeQuery()) {
				if (resultSet.next()) {
					totalDeleted = resultSet.getInt("total_deleted");
				}
			}

			if (totalDeleted > 0) {
				try (PreparedStatement analyticsStatement = connection.prepareStatement("UPDATE bugreport_analytics SET total_deleted = ?")) {
					analyticsStatement.setInt(1, totalDeleted + 1);
					int updateRowsAffected = analyticsStatement.executeUpdate();
					if (debugMode) {
						plugin.getLogger().info("Updated total_deleted, rows affected: " + updateRowsAffected);
						plugin.getLogger().info("Updated bug report total deleted to " + (totalDeleted + 1));
					}
				}
			} else {
				try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO bugreport_analytics (total_deleted) VALUES (?)")) {
					insertStatement.setInt(1, 1);
					int insertRowsAffected = insertStatement.executeUpdate();
					if (debugMode) {
						plugin.getLogger().info("Inserted initial bug report total_deleted value of 1, rows affected: " + insertRowsAffected);
					}
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to delete bug report or update analytics.");
			plugin.getLogger().severe(e.getMessage());
		}

		loadBugReports();
	}

	public static void addBugReport(String username, @NotNull UUID playerId, String world, String header, String fullMessage, String location, String gamemode, String serverName, String discordWebhookMessageID) {
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO bug_reports(player_id, header, message, username, world, archived, report_id, timestamp, location, gamemode, status, serverName, discordWebhookMessageID) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			int report_id = 1;
			ResultSet resultSet = connection.createStatement().executeQuery("SELECT report_id FROM bug_reports ORDER BY report_id DESC LIMIT 1");
			if (resultSet.next()) {
				report_id = resultSet.getInt("report_id") + 1;
			}
			statement.setString(1, playerId.toString());
			statement.setString(2, header);
			statement.setString(3, fullMessage);
			statement.setString(4, username);
			statement.setString(5, world);
			statement.setInt(6, 0);
			statement.setInt(7, report_id);
			statement.setLong(8, System.currentTimeMillis());
			statement.setString(9, location);
			statement.setString(10, gamemode);
			statement.setString(11, "0");
			statement.setString(12, serverName);
			statement.setString(13, discordWebhookMessageID);

			if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {
				PlanHook.getInstance().updateHook(playerId, username);
			}

			statement.executeUpdate();
			statement.close();
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to add bug report.");
			plugin.getLogger().severe(e.getMessage());
		}
	}

	public static @Nullable String getBugReportDiscordWebhookMessageID(Integer reportIDGUI) {
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement("SELECT discordWebhookMessageID FROM bug_reports WHERE report_id = ?");
			statement.setInt(1, reportIDGUI);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("discordWebhookMessageID");
			}
			statement.close();
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to get bug report discord webhook message ID.");
			plugin.getLogger().severe(e.getMessage());
		}
		return null;
	}

	private void makeAllHeadersEqualReport_ID() {
		try (Connection connection = dataSource.getConnection()) {
			ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM bug_reports");
			while (resultSet.next()) {
				int report_id = resultSet.getInt("report_id");
				String header = resultSet.getString("header");
				String[] lines = header.split("\n");
				StringBuilder newHeader = new StringBuilder();
				for (String line : lines) {
					if (line.startsWith("Report ID:")) {
						newHeader.append("Report ID: ").append(report_id);
					} else {
						newHeader.append(line);
					}
					newHeader.append("\n");
				}
				PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET header = ? WHERE report_id = ?");
				statement.setString(1, newHeader.toString().trim());
				statement.setInt(2, report_id);
				statement.executeUpdate();
				statement.close();
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to make all headers equal report_id.");
			plugin.getLogger().severe(e.getMessage());
		}
	}

	private void fixReportID() {
		try (Connection connection = dataSource.getConnection()) {
			ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM bug_reports WHERE report_id IS NULL OR report_id = 0");
			while (resultSet.next()) {
				int report_id = resultSet.getInt("report_id");
				int rowNumber = resultSet.getRow();
				if (report_id != rowNumber) {
					PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET report_id = ? WHERE report_id = ?");
					statement.setInt(1, rowNumber);
					statement.setInt(2, report_id);
					statement.executeUpdate();
					statement.close();
				}
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to fix report_id.");
			plugin.getLogger().severe(e.getMessage());
		}

	}
}
