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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportManager.*;

public class BugReportDatabase {
	public static HikariDataSource dataSource;
	private static final UUID STATIC_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private static final Map<Integer, String> REPORT_CACHE = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LOGIN_TIMESTAMP_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Long> COUNT_CACHE = new ConcurrentHashMap<>();
	private static final AtomicLong CACHE_EXPIRY = new AtomicLong(System.currentTimeMillis() + 60000);

	public BugReportDatabase() {
		createConnection();
		initializeDatabaseSchema();
	}

	private void initializeDatabaseSchema() {
		String[] columns = {
				"player_data|player_id|TEXT, last_login_timestamp BIGINT DEFAULT 0",
				"bug_reports|archived|INTEGER DEFAULT 0",
				"bug_reports|report_id|INT AUTO_INCREMENT PRIMARY KEY",
				"bug_reports|location|TEXT",
				"bug_reports|gamemode|TEXT",
				"bug_reports|serverName|TEXT"
		};

		try (Connection conn = dataSource.getConnection()) {
			Arrays.stream(columns)
					.parallel()
					.forEach(col -> {
						String[] parts = col.split("\\|");
						addColumnIfNotExists(parts[0], parts[1], parts[2]);
					});

			addTimestampColumns(conn);
			fixReportID(conn);
			makeAllHeadersEqualReport_ID(conn);
		} catch (SQLException e) {
			logError(30, e.getMessage());
		}
	}

	public static void reloadConnection() {
		clearCaches();
		if (dataSource != null) {
			try {
				dataSource.close();
			} catch (Exception ignored) {
			}
		}
		createConnection();
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
			logError(15, null);
		}
	}

	private static void connectRemote(String host, Integer port, String database, String username, String password) {
		HikariConfig hikariConfig = new HikariConfig();
		try {
			hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&rewriteBatchedStatements=true");
			hikariConfig.setUsername(username);
			hikariConfig.setPassword(password);

			hikariConfig.setMaximumPoolSize(10);
			hikariConfig.setMinimumIdle(5);
			hikariConfig.setConnectionTimeout(10000);
			hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
			hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
			hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

			dataSource = new HikariDataSource(hikariConfig);
			plugin.getLogger().info("Connected to remote database");
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to connect to remote database: " + e.getMessage());
		}
		createTables();
	}

	private static void connectLocal() {
		try {
			File databaseFile = new File("plugins/BugReport/bugreports.db");
			if (!databaseFile.exists() && databaseFile.createNewFile()) {
				plugin.getLogger().info("Created local database file");
			}

			HikariConfig hikariConfig = new HikariConfig();
			hikariConfig.setDriverClassName("org.sqlite.JDBC");
			hikariConfig.setConnectionTestQuery("SELECT 1");
			hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());

			hikariConfig.setMaximumPoolSize(5);
			hikariConfig.setMinimumIdle(2);
			hikariConfig.addDataSourceProperty("pragma.synchronous", "OFF");
			hikariConfig.addDataSourceProperty("pragma.journal_mode", "WAL");
			hikariConfig.addDataSourceProperty("pragma.page_size", "4096");
			hikariConfig.addDataSourceProperty("pragma.cache_size", "10000");

			dataSource = new HikariDataSource(hikariConfig);
			plugin.getLogger().info("Connected to local database");
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to connect to local database: " + e.getMessage());
		}
		createTables();
	}

	private static void createTables() {
		try (Connection conn = dataSource.getConnection()) {
			String[] tableQueries = {
					"CREATE TABLE IF NOT EXISTS bug_reports(rowid INTEGER, player_id TEXT, header TEXT, message TEXT, username TEXT, world TEXT, archived INTEGER DEFAULT 0, report_id INTEGER, timestamp BIGINT, status TEXT, serverName TEXT, discordWebhookMessageID TEXT)",
					"CREATE TABLE IF NOT EXISTS player_data(player_id TEXT, last_login_timestamp BIGINT DEFAULT 0)",
					"CREATE TABLE IF NOT EXISTS bugreport_analytics(total_deleted INTEGER DEFAULT 0)"
			};

			for (String query : tableQueries) {
				conn.createStatement().execute(query);
			}

			String[] indexQueries = {
					"CREATE INDEX IF NOT EXISTS idx_bug_reports_player_id ON bug_reports(player_id)",
					"CREATE INDEX IF NOT EXISTS idx_bug_reports_report_id ON bug_reports(report_id)",
					"CREATE INDEX IF NOT EXISTS idx_bug_reports_archived ON bug_reports(archived)",
					"CREATE INDEX IF NOT EXISTS idx_player_data_player_id ON player_data(player_id)"
			};

			for (String query : indexQueries) {
				try {
					conn.createStatement().execute(query);
				} catch (SQLException ignored) {
				}
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
		}
	}

	private static void addTimestampColumns(Connection conn) {
		try {
			addColumnIfExists(conn, "player_data", "last_login_timestamp", "BIGINT DEFAULT 0");
			addColumnIfExists(conn, "bug_reports", "timestamp", "BIGINT");
			addColumnIfExists(conn, "bug_reports", "discordWebhookMessageID", "TEXT");
		} catch (SQLException e) {
			logError(35, e.getMessage());
		}
	}

	private static void addColumnIfExists(Connection conn, String table, String column, String definition) throws SQLException {
		try (ResultSet resultSet = conn.getMetaData().getColumns(null, null, table, column)) {
			if (!resultSet.next()) {
				conn.createStatement().execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
			}
		}
	}

	private static void addColumnIfNotExists(String connTableName, String connColumnName, String connColumnDefinition) {
		try (Connection conn = dataSource.getConnection()) {
			try (ResultSet resultSet = conn.getMetaData().getColumns(null, null, connTableName, connColumnName)) {
				if (!resultSet.next()) {
					String query = String.format("ALTER TABLE %s ADD COLUMN %s %s", connTableName, connColumnName, connColumnDefinition);
					conn.createStatement().execute(query);
				}
			}
		} catch (Exception e) {
			logError(39, e.getMessage());
		}
	}

	private void fixReportID(Connection conn) {
		try {
			try (ResultSet resultSet = conn.createStatement().executeQuery(
					"SELECT * FROM bug_reports WHERE report_id IS NULL OR report_id = 0")) {

				if (resultSet.next()) {
					try (PreparedStatement update = conn.prepareStatement(
							"UPDATE bug_reports SET report_id = ? WHERE rowid = ?")) {

						conn.setAutoCommit(false);
						int batchCount = 0;

						do {
							int rowId = resultSet.getInt("rowid");
							int rowNumber = resultSet.getRow();

							update.setInt(1, rowNumber);
							update.setInt(2, rowId);
							update.addBatch();

							if (++batchCount % 100 == 0) {
								update.executeBatch();
							}
						} while (resultSet.next());

						if (batchCount % 100 != 0) {
							update.executeBatch();
						}

						conn.commit();
						conn.setAutoCommit(true);
					}
				}
			}
		} catch (SQLException e) {
			try {
				conn.rollback();
				conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
			plugin.getLogger().severe("Failed to fix report IDs: " + e.getMessage());
		}
	}

	private void makeAllHeadersEqualReport_ID(Connection conn) {
		try {
			try (ResultSet resultSet = conn.createStatement().executeQuery("SELECT report_id, header FROM bug_reports")) {
				if (resultSet.next()) {
					try (PreparedStatement update = conn.prepareStatement(
							"UPDATE bug_reports SET header = ? WHERE report_id = ?")) {

						conn.setAutoCommit(false);
						int batchCount = 0;

						do {
							int reportId = resultSet.getInt("report_id");
							String header = resultSet.getString("header");
							String updatedHeader = updateHeaderReportId(header, reportId);

							update.setString(1, updatedHeader);
							update.setInt(2, reportId);
							update.addBatch();

							if (++batchCount % 100 == 0) {
								update.executeBatch();
							}
						} while (resultSet.next());

						if (batchCount % 100 != 0) {
							update.executeBatch();
						}

						conn.commit();
						conn.setAutoCommit(true);
					}
				}
			}
		} catch (SQLException e) {
			try {
				conn.rollback();
				conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
			plugin.getLogger().severe("Failed to update report headers: " + e.getMessage());
		}
	}

	private String updateHeaderReportId(String header, int reportId) {
		return Arrays.stream(header.split("\n"))
				.map(line -> line.startsWith("Report ID:") ? "Report ID: " + reportId : line)
				.collect(Collectors.joining("\n"));
	}

	public static void setPlayerLastLoginTimestamp(UUID playerId) {
		long currentTime = System.currentTimeMillis();
		Long cachedTimestamp = LOGIN_TIMESTAMP_CACHE.get(playerId);

		if (cachedTimestamp != null && currentTime - cachedTimestamp < 300000) {
			return;
		}

		LOGIN_TIMESTAMP_CACHE.put(playerId, currentTime);

		long lastLogin = getPlayerLastLoginTimestamp(playerId);
		String sql = lastLogin == 0
				? "INSERT INTO player_data(player_id, last_login_timestamp) VALUES(?, ?)"
				: "UPDATE player_data SET last_login_timestamp = ? WHERE player_id = ?";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {

			if (lastLogin == 0) {
				stmt.setString(1, playerId.toString());
				stmt.setLong(2, currentTime);
			} else {
				stmt.setLong(1, currentTime);
				stmt.setString(2, playerId.toString());
			}
			stmt.executeUpdate();
		} catch (SQLException e) {
			logError(36, e.getMessage());
		}
	}

	public static long getPlayerLastLoginTimestamp(UUID playerId) {
		if (LOGIN_TIMESTAMP_CACHE.containsKey(playerId)) {
			return LOGIN_TIMESTAMP_CACHE.get(playerId);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT last_login_timestamp FROM player_data WHERE player_id = ?")) {

			stmt.setString(1, playerId.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					long timestamp = rs.getLong("last_login_timestamp");
					LOGIN_TIMESTAMP_CACHE.put(playerId, timestamp);
					return timestamp;
				}
			}
		} catch (SQLException e) {
			logError(37, e.getMessage());
		}

		LOGIN_TIMESTAMP_CACHE.put(playerId, 0L);
		return 0;
	}

	public static void addBugReport(String username, @NotNull UUID playerId, String world,
									String header, String fullMessage, String location,
									String gamemode, String serverName, String discordWebhookMessageID) {
		try (Connection conn = dataSource.getConnection()) {
			int reportId = getNextReportId(conn);

			try (PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO bug_reports(player_id, header, message, username, world, archived, " +
							"report_id, timestamp, location, gamemode, status, serverName, discordWebhookMessageID) " +
							"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

				long currentTime = System.currentTimeMillis();
				stmt.setString(1, playerId.toString());
				stmt.setString(2, header);
				stmt.setString(3, fullMessage);
				stmt.setString(4, username);
				stmt.setString(5, world);
				stmt.setInt(6, 0);
				stmt.setInt(7, reportId);
				stmt.setLong(8, currentTime);
				stmt.setString(9, location);
				stmt.setString(10, gamemode);
				stmt.setString(11, "0");
				stmt.setString(12, serverName);
				stmt.setString(13, discordWebhookMessageID);
				stmt.executeUpdate();
			}

			if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {
				PlanHook.getInstance().updateHook(playerId, username);
			}

			clearCaches();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to add bug report: " + e.getMessage());
		}
	}

	public static void updateReportStatus(int reportId, int statusId) {
		if (debugMode) {
			plugin.getLogger().info("Updating report status for report ID " + reportId);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "UPDATE bug_reports SET status = ? WHERE report_id = ?")) {
			stmt.setInt(1, statusId);
			stmt.setInt(2, reportId);
			stmt.executeUpdate();

			updateReportInMemory(reportId, "Status", String.valueOf(statusId));
			REPORT_CACHE.remove(reportId);
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to update bug report status: " + e.getMessage());
		}

		if (debugMode) {
			plugin.getLogger().info("Updated report status for report ID " + reportId);
		}
	}

	public static void updateBugReportArchive(int reportId, int archived) {
		if (debugMode) {
			plugin.getLogger().info("Updating bug report archive status for report ID " + reportId);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "UPDATE bug_reports SET archived = ? WHERE report_id = ?")) {
			stmt.setInt(1, archived);
			stmt.setInt(2, reportId);
			stmt.executeUpdate();

			updateReportInMemory(reportId, "Archived", String.valueOf(archived));
			REPORT_CACHE.remove(reportId);
			clearCountCaches();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to update bug report archive status: " + e.getMessage());
		}

		if (debugMode) {
			plugin.getLogger().info("Updated bug report archive status for report ID " + reportId);
		}
	}

	public static void deleteBugReport(int reportId) {
		if (debugMode) {
			plugin.getLogger().info("Deleting bug report for report ID " + reportId);
		}

		try (Connection conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);

			try (PreparedStatement stmt = conn.prepareStatement(
					"DELETE FROM bug_reports WHERE report_id = ?")) {
				stmt.setInt(1, reportId);
				int rowsAffected = stmt.executeUpdate();

				if (debugMode) {
					plugin.getLogger().info("Deleted bug report rows affected: " + rowsAffected);
				}
			}

			updateDeletedReportCount(conn);
			conn.commit();

			REPORT_CACHE.remove(reportId);
			clearCountCaches();
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to delete bug report: " + e.getMessage());
		}
	}

	private static void updateDeletedReportCount(Connection conn) throws SQLException {
		int totalDeleted = 0;

		try (PreparedStatement query = conn.prepareStatement("SELECT total_deleted FROM bugreport_analytics");
			 ResultSet rs = query.executeQuery()) {
			if (rs.next()) {
				totalDeleted = rs.getInt("total_deleted");
			}
		}

		String sql = totalDeleted > 0
				? "UPDATE bugreport_analytics SET total_deleted = ?"
				: "INSERT INTO bugreport_analytics (total_deleted) VALUES (?)";

		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, totalDeleted + 1);
			stmt.executeUpdate();
		}
	}

	public static @NotNull Map<String, String> getBugReportById(int reportId) {
		if (REPORT_CACHE.containsKey(reportId)) {
			String cachedReport = REPORT_CACHE.get(reportId);
			if (cachedReport != null) {
				Map<String, String> parsedReport = parseReportData(cachedReport);
				return parsedReport;
			}
		}

		Map<String, String> reportData = new HashMap<>();

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM bug_reports WHERE report_id = ?")) {
			stmt.setInt(1, reportId);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					reportData.put("Username", rs.getString("username"));
					reportData.put("UUID", rs.getString("player_id"));
					reportData.put("World", rs.getString("world"));
					reportData.put("FullMessage", rs.getString("message"));
					reportData.put("Header", rs.getString("header"));
					reportData.put("Location", rs.getString("location"));
					reportData.put("Gamemode", rs.getString("gamemode"));
					reportData.put("Status", rs.getString("status"));
					reportData.put("ServerName", rs.getString("serverName"));
					reportData.put("CategoryID", getCategoryFromHeader(rs.getString("header")));

					String cacheString = formatReportDataForCache(reportData);
					REPORT_CACHE.put(reportId, cacheString);
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to get bug report by ID: " + e.getMessage());
		}

		return reportData;
	}

	private static @NotNull String getCategoryFromHeader(String header) {
		if (header == null) {
			return "Unknown";
		}

		String[] lines = header.split("\n");
		for (String line : lines) {
			if (line.startsWith("Category ID: ")) {
				return line.substring(13);
			}
		}
		return "Unknown";
	}

	private static @NotNull Map<String, String> parseReportData(String cacheString) {
		Map<String, String> reportData = new HashMap<>();
		if (cacheString == null) {
			return reportData;
		}

		// Check if the cache string is in key=value||key=value format
		if (cacheString.contains("||") && cacheString.contains("=")) {
			String[] keyValuePairs = cacheString.split("\\|\\|");
			for (String keyValuePair : keyValuePairs) {
				String[] parts = keyValuePair.split("=", 2);
				if (parts.length == 2) {
					reportData.put(parts[0], parts[1]);
				}
			}
		} else {
			// Assume it's in multi-line format with "Key: Value"
			String[] lines = cacheString.split("\n");
			for (String line : lines) {
				int colonIndex = line.indexOf(": ");
				if (colonIndex > 0) {
					String key = line.substring(0, colonIndex);
					String value = line.substring(colonIndex + 2);
					reportData.put(key, value);
				}
			}
		}
		return reportData;
	}

	private static String formatReportDataForCache(Map<String, String> reportData) {
		return reportData.entrySet().stream()
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining("||"));
	}

	public static @NotNull String getBugReportById(int reportId, boolean isArchived) {
		String cacheKey = reportId + "_" + isArchived;
		if (REPORT_CACHE.containsKey(reportId)) {
			return REPORT_CACHE.get(reportId);
		}

		StringBuilder reportBuilder = new StringBuilder(512);

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT * FROM bug_reports WHERE report_id = ? AND archived = ?")) {

			stmt.setInt(1, reportId);
			stmt.setInt(2, isArchived ? 1 : 0);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					reportBuilder.append("Username: ").append(rs.getString("username")).append('\n')
							.append("UUID: ").append(rs.getString("player_id")).append('\n')
							.append("World: ").append(rs.getString("world")).append('\n')
							.append("Full Message: ").append(rs.getString("message")).append('\n')
							.append("Header: ").append(rs.getString("header")).append('\n')
							.append("Archived: ").append(rs.getString("archived")).append('\n')
							.append("Report ID: ").append(rs.getString("report_id")).append('\n')
							.append("Timestamp: ").append(rs.getLong("timestamp")).append('\n')
							.append("Location: ").append(rs.getString("location")).append('\n')
							.append("Gamemode: ").append(rs.getString("gamemode")).append('\n')
							.append("Status: ").append(rs.getString("status")).append('\n')
							.append("Server Name: ").append(rs.getString("serverName"));

					String result = reportBuilder.toString();
					REPORT_CACHE.put(reportId, result);
					return result;
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to get bug report by ID: " + e.getMessage());
		}

		return reportBuilder.toString();
	}

	public static @Nullable Location getBugReportLocation(Integer reportId) {
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT location FROM bug_reports WHERE report_id = ?")) {

			stmt.setInt(1, reportId);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					String locationString = rs.getString("Location");
					if (locationString != null) {
						String[] locationSplit = locationString.split(",");
						return new Location(
								Bukkit.getWorld(locationSplit[0]),
								Double.parseDouble(locationSplit[1]),
								Double.parseDouble(locationSplit[2]),
								Double.parseDouble(locationSplit[3])
						);
					}
				}
			}
		} catch (SQLException e) {
			logError(38, e.getMessage());
		}

		return null;
	}

	public static @Nullable String getBugReportDiscordWebhookMessageID(Integer reportId) {
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT discordWebhookMessageID FROM bug_reports WHERE report_id = ?")) {

			stmt.setInt(1, reportId);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getString("discordWebhookMessageID");
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe("Failed to get discord webhook ID: " + e.getMessage());
		}

		return null;
	}

	public static long loadDeletedBugReportCount() {
		String cacheKey = "deleted_reports";

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT total_deleted FROM bugreport_analytics");
			 ResultSet rs = stmt.executeQuery()) {

			if (rs.next()) {
				long count = rs.getLong("total_deleted");
				COUNT_CACHE.put(cacheKey, count);
				return count;
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static long loadBugReportCountForPlayer(@NotNull UUID playerId) {
		String cacheKey = "player_reports_" + playerId;

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT COUNT(*) FROM bug_reports WHERE player_id = ?")) {

			stmt.setString(1, playerId.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					long count = rs.getLong(1);
					COUNT_CACHE.put(cacheKey, count);
					return count;
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static long loadArchivedBugReportCountForPlayer(@NotNull UUID playerId) {
		String cacheKey = "player_archived_" + playerId;

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT COUNT(*) FROM bug_reports WHERE player_id = ? AND archived = 1")) {

			stmt.setString(1, playerId.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					long count = rs.getLong(1);
					COUNT_CACHE.put(cacheKey, count);
					return count;
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static long loadNonArchivedBugReportCountForPlayer(@NotNull UUID playerId) {
		String cacheKey = "player_nonarchived_" + playerId;

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT COUNT(*) FROM bug_reports WHERE player_id = ? AND archived = 0")) {

			stmt.setString(1, playerId.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					long count = rs.getLong(1);
					COUNT_CACHE.put(cacheKey, count);
					return count;
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static long loadBugReportCount() {
		String cacheKey = "total_reports";

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM bug_reports");
			 ResultSet rs = stmt.executeQuery()) {

			if (rs.next()) {
				long count = rs.getLong(1);
				COUNT_CACHE.put(cacheKey, count);
				return count;
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static long loadArchivedBugReportCount() {
		String cacheKey = "archived_reports";

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE archived = 1");
			 ResultSet rs = stmt.executeQuery()) {

			if (rs.next()) {
				long count = rs.getLong(1);
				COUNT_CACHE.put(cacheKey, count);
				return count;
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static long loadNonArchivedBugReportCount() {
		String cacheKey = "nonarchived_reports";

		if (COUNT_CACHE.containsKey(cacheKey) && System.currentTimeMillis() < CACHE_EXPIRY.get()) {
			return COUNT_CACHE.get(cacheKey);
		}

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM bug_reports WHERE archived = 0");
			 ResultSet rs = stmt.executeQuery()) {

			if (rs.next()) {
				long count = rs.getLong(1);
				COUNT_CACHE.put(cacheKey, count);
				return count;
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return 0;
	}

	public static @NotNull List<BugReportPair<String, String>> loadBugReportCountsPerPlayer() {
		List<BugReportPair<String, String>> reports = new ArrayList<>();

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT username, message FROM bug_reports");
			 ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				reports.add(new BugReportPair<>(
						rs.getString("username"),
						rs.getString("message")
				));
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return reports;
	}

	public static @NotNull List<BugReportPair<String, String>> loadBugReportAllPlayer(String playerName) {
		List<BugReportPair<String, String>> reports = new ArrayList<>();

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT timestamp, message FROM bug_reports WHERE username = ?")) {

			stmt.setString(1, playerName);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					reports.add(new BugReportPair<>(
							translateTimestampToDate(rs.getLong("timestamp")),
							rs.getString("message")
					));
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().severe(e.getMessage());
		}

		return reports;
	}

	public static @NotNull Map<UUID, List<String>> loadBugReports() {
		if (debugMode) {
			plugin.getLogger().info("Loading bug reports");
		}

		Map<UUID, List<String>> bugReports = new HashMap<>();
		List<String> reports = new ArrayList<>(Collections.singletonList("DUMMY"));
		bugReports.put(STATIC_UUID, reports);

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "SELECT * FROM bug_reports ORDER BY report_id ASC");
			 ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				UUID playerId = UUID.fromString(rs.getString("player_id"));
				String reportData = formatReportData(rs);
				reports.add(reportData);

				if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {
					PlanHook.getInstance().updateHook(playerId, rs.getString("username"));
				}

				int reportId = rs.getInt("report_id");
				REPORT_CACHE.put(reportId, reportData);
			}
		} catch (SQLException e) {
			plugin.getLogger().severe("Database error: " + e.getMessage());

			if (e.getMessage().startsWith("[SQLITE_CORRUPT]")) {
				String errorMessageCorrupt = ErrorMessages.getErrorMessage(41);
				plugin.getLogger().severe(errorMessageCorrupt);
				plugin.getLogger().severe("If you need help, please join the discord server: https://discord.gg/ZvdNYqmsbx");
			}
		}

		if (debugMode) {
			plugin.getLogger().info("Loaded " + (reports.size() - 1) + " bug reports");
		}

		return bugReports;
	}

	public static UUID getStaticUUID() {
		return STATIC_UUID;
	}

	private static @NotNull String formatReportData(@NotNull ResultSet rs) throws SQLException {
		return "Username: " + rs.getString("username") + "\n" +
				"UUID: " + rs.getString("player_id") + "\n" +
				"World: " + rs.getString("world") + "\n" +
				"Full Message: " + rs.getString("message") + "\n" +
				"Header: " + rs.getString("header") + "\n" +
				"Archived: " + rs.getString("archived") + "\n" +
				"Report ID: " + rs.getString("report_id") + "\n" +
				"Timestamp: " + rs.getLong("timestamp") + "\n" +
				"Location: " + rs.getString("location") + "\n" +
				"Gamemode: " + rs.getString("gamemode") + "\n" +
				"Status: " + rs.getString("status") + "\n" +
				"Server Name: " + rs.getString("serverName");
	}

	private static void updateReportInMemory(int reportId, String fieldName, String newValue) {
		List<String> reports = bugReports.getOrDefault(STATIC_UUID,
				new ArrayList<>(Collections.singletonList("DUMMY")));

		String reportToUpdate = reports.stream()
				.filter(report -> report.contains("Report ID: " + reportId))
				.findFirst()
				.orElse(null);

		if (reportToUpdate != null) {
			int position = reports.indexOf(reportToUpdate);

			String pattern = fieldName + ": .*?(\\n|$)";
			String replacement = fieldName + ": " + newValue + "$1";
			String updatedReport = reportToUpdate.replaceAll(pattern, replacement);

			reports.set(position, updatedReport);
			bugReports.put(STATIC_UUID, reports);
			REPORT_CACHE.put(reportId, updatedReport);
		}
	}

	private static int getNextReportId(Connection conn) throws SQLException {
		try (PreparedStatement stmt = conn.prepareStatement(
				"SELECT MAX(report_id) FROM bug_reports")) {
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? rs.getInt(1) + 1 : 1;
			}
		}
	}

	private static @NotNull String getField(@NotNull String input, String fieldName) {
		int startIdx = input.indexOf(fieldName + ": ");
		if (startIdx != -1) {
			startIdx += fieldName.length() + 2;
			int endIdx = input.indexOf('\n', startIdx);
			if (endIdx != -1) {
				return input.substring(startIdx, endIdx);
			} else {
				return input.substring(startIdx);
			}
		}
		return "Unknown";
	}

	private static void logError(int errorCode, String additionalMessage) {
		String errorMessage = additionalMessage != null
				? ErrorMessages.getErrorMessageWithAdditionalMessage(errorCode, additionalMessage)
				: ErrorMessages.getErrorMessage(errorCode);

		plugin.getLogger().severe(errorMessage);
		logErrorMessage(errorMessage);
	}

	private static void clearCaches() {
		REPORT_CACHE.clear();
		clearCountCaches();
		LOGIN_TIMESTAMP_CACHE.clear();
		CACHE_EXPIRY.set(System.currentTimeMillis() + 60000);
	}

	private static void clearCountCaches() {
		COUNT_CACHE.clear();
	}
}
