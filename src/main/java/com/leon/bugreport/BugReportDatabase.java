package com.leon.bugreport;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.leon.bugreport.BugReportManager.*;

public class BugReportDatabase {
    public static HikariDataSource dataSource;

    public BugReportDatabase() {
        createConnection();
        addMissingTables();
        fixReportID();
        makeAllHeadersEqualReport_ID();
        addTimestampColumn();
    }

    private static void addTimestampColumn() {
        try (Connection connection = dataSource.getConnection()) {
            try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "player_data",
                    "last_login_timestamp")) {
                if (!archivedResultSet.next()) {
                    connection.createStatement()
                            .execute("ALTER TABLE player_data ADD COLUMN last_login_timestamp BIGINT DEFAULT 0");
                }
            }
            try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "bug_reports",
                    "timestamp")) {
                if (!archivedResultSet.next()) {
                    connection.createStatement()
                            .execute("ALTER TABLE bug_reports ADD COLUMN timestamp BIGINT");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add missing columns.");
            plugin.getLogger().severe(e.getMessage());
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
            plugin.getLogger().severe("Failed to set player last login timestamp.");
            plugin.getLogger().severe(e.getMessage());
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
            plugin.getLogger().severe("Failed to get player last login timestamp.");
            plugin.getLogger().severe(e.getMessage());
        }
        return 0;
    }

    private void addMissingTables() {
        try (Connection connection = dataSource.getConnection()) {
            try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "player_data",
                    "player_id")) {
                if (!archivedResultSet.next()) {
                    connection.createStatement()
                            .execute("CREATE TABLE IF NOT EXISTS player_data(player_id TEXT, last_login_timestamp BIGINT DEFAULT 0)");
                }
            }
            try (ResultSet archivedResultSet = connection.getMetaData().getColumns(null, null, "bug_reports",
                    "archived")) {
                if (!archivedResultSet.next()) {
                    connection.createStatement()
                            .execute("ALTER TABLE bug_reports ADD COLUMN archived INTEGER DEFAULT 0");
                }
            }
            try (ResultSet reportIdResultSet = connection.getMetaData().getColumns(null, null, "bug_reports",
                    "report_id")) {
                if (!reportIdResultSet.next()) {
                    connection.createStatement()
                            .execute("ALTER TABLE bug_reports ADD COLUMN report_id INT AUTO_INCREMENT PRIMARY KEY");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add missing columns.");
            plugin.getLogger().severe(e.getMessage());
        }
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
                PreparedStatement statement = connection
                        .prepareStatement("UPDATE bug_reports SET header = ? WHERE report_id = ?");
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
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT * FROM bug_reports WHERE report_id IS NULL OR report_id = 0");
            while (resultSet.next()) {
                int report_id = resultSet.getInt("report_id");
                int rowNumber = resultSet.getRow();
                if (report_id != rowNumber) {
                    PreparedStatement statement = connection
                            .prepareStatement("UPDATE bug_reports SET report_id = ? WHERE report_id = ?");
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

    private static void createConnection() {
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
            plugin.getLogger().warning("Invalid database type. Please use 'local' or 'mysql'.");
        }
    }

    public void addBugReport(String username, @NotNull UUID playerId, String world, String header, String fullMessage) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bug_reports(player_id, header, message, username, world, archived, report_id, timestamp) VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            int report_id = 1;
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT report_id FROM bug_reports ORDER BY report_id DESC LIMIT 1");
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
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add bug report.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public static @NotNull Map<UUID, List<String>> loadBugReports() {
        Map<UUID, List<String>> bugReports = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection
                    .prepareStatement("SELECT * FROM bug_reports ORDER BY report_id ASC");
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
                List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
                reports.add(
                        "Username: " + username + "\n" +
                                "UUID: " + playerId + "\n" +
                                "World: " + world + "\n" +
                                "Full Message: " + fullMessage + "\n" +
                                "Header: " + header + "\n" +
                                "Archived: " + archived + "\n" +
                                "Report ID: " + report_id + "\n" +
                                "Timestamp: " + timestamp
                );
                bugReports.put(getStaticUUID(), reports);
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load bug reports.");
            plugin.getLogger().severe(e.getMessage());
        }

        return bugReports;
    }

    static @NotNull UUID getStaticUUID() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private static void connectRemote(String host, Integer port, String database, String username, String password) {
        HikariConfig hikariConfig = new HikariConfig();
        try {
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to remote database.");
            plugin.getLogger().severe(e.getMessage());
        }

        plugin.getLogger().info("Connected to remote database");
        createTables();
    }

    private static void connectLocal() {
        try {
            File databaseFile = new File("plugins/BugReport/bugreports.db");
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to local database.");
            plugin.getLogger().severe(e.getMessage());
        }

        plugin.getLogger().info("Connected to local database");
        createTables();
    }

    private static void createTables() {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS bug_reports(rowid INTEGER, player_id TEXT, header TEXT, message TEXT, username TEXT, world TEXT, archived INTEGER DEFAULT 0, report_id INTEGER, timestamp BIGINT)");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create tables.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public void updateBugReportHeader(UUID playerId, int reportIndex) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection
                    .prepareStatement("UPDATE bug_reports SET header = ? WHERE report_id = ?");
            String existingHeader = bugReports.get(playerId).get(reportIndex);

            String[] lines = existingHeader.split("\n");
            StringBuilder newHeader = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith("hasBeenRead:")) {
                    newHeader.append("hasBeenRead: 1");
                } else {
                    newHeader.append(line);
                }
                newHeader.append("\n");
            }

            statement.setString(1, newHeader.toString().trim());
            statement.setInt(2, reportIndex);
            statement.executeUpdate();
            statement.close();
            loadBugReports();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update bug report read status.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public static void updateBugReportArchive(int reportIndex, int archived) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET archived = ? WHERE report_id = ?");
            statement.setInt(1, archived);
            statement.setInt(2, reportIndex);
            statement.executeUpdate();
            statement.close();
            loadBugReports();

            List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
            String existingHeader = reports.stream()
                    .filter(reportString -> reportString.contains("Report ID: " + reportIndex))
                    .findFirst()
                    .orElse(null);
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

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update bug report archive status.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public static void deleteBugReport(int reportIndex) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM bug_reports WHERE report_id = ?");
            statement.setInt(1, reportIndex);
            statement.executeUpdate();
            statement.close();
            loadBugReports();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to delete bug report.");
            plugin.getLogger().severe(e.getMessage());
        }
    }
}
