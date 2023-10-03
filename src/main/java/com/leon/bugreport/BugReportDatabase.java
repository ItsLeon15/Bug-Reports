package com.leon.bugreport;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.leon.bugreport.BugReportManager.*;

public class BugReportDatabase {
    private static Connection connection;

    public BugReportDatabase() {
        connectLocalOrRemote();
        addMissingTables();
        fixReportID();
    }

    private void addMissingTables() {
        try {
            ResultSet archivedResultSet = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'bug_reports' AND COLUMN_NAME = 'archived'"
            );
            ResultSet reportIdResultSet = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'bug_reports' AND COLUMN_NAME = 'report_id'"
            );

            if (archivedResultSet.next() && archivedResultSet.getInt(1) == 0) {
                connection.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN archived INTEGER DEFAULT 0");
            }
            if (reportIdResultSet.next() && reportIdResultSet.getInt(1) == 0) {
                connection.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN report_id INT AUTO_INCREMENT PRIMARY KEY");
            }
        } catch(SQLException e) {
            plugin.getLogger().severe("Failed to add missing columns.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private void fixReportID() {
        try {
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
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fix report_id.");
            plugin.getLogger().severe(e.getMessage());
        }
    }


    private void connectLocalOrRemote() {
        loadConfig();

        String databaseType = Objects.requireNonNull(config.getString("databaseType"));
        ConfigurationSection databaseSection = config.getConfigurationSection("database");

        if (databaseType.equalsIgnoreCase("local")) {
            System.out.println("Connecting to local database");
            connectLocal();
        } else if (databaseType.equalsIgnoreCase("mysql")) {
            System.out.println("Connecting to remote database");

            String host = databaseSection.getString("host");
            int port = databaseSection.getInt("port");
            String database = databaseSection.getString("database");
            String username = databaseSection.getString("username");
            String password = databaseSection.getString("password");

            connectRemote(host, port, database, username, password);
        } else {
            System.out.println("Invalid database type. Please use 'local' or 'mysql'.");
        }
    }

    public void addBugReport(String username, @NotNull UUID playerId, String world, String header, String fullMessage) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO bug_reports(player_id, header, message, username, world, archived, report_id) VALUES(?, ?, ?, ?, ?, ?, ?)");

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

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add bug report.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public Map<UUID, List<String>> loadBugReports() {
        Map<UUID, List<String>> bugReports = new HashMap<>();

        try {
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

                List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());
                reports.add(
                        "Username: " + username + "\n" +
                        "UUID: " + playerId + "\n" +
                        "World: " + world + "\n" +
                        "Full Message: " + fullMessage + "\n" +
                        "Header: " + header + "\n" +
                        "Archived: " + archived + "\n" +
                        "Report ID: " + report_id
                );
                bugReports.put(playerId, reports);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load bug reports.");
            plugin.getLogger().severe(e.getMessage());
        }

        return bugReports;
    }

    private void connectRemote(String host, Integer port, String database, String username, String password) {
        try {
            String databaseURL = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
            connection = DriverManager.getConnection(databaseURL, username, password);
            System.out.println("Connected to remote database");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to remote database.");
            plugin.getLogger().severe(e.getMessage());
        }

        createTables();
    }

    private void connectLocal() {
        try {
            File databaseFile = new File("plugins/BugReport/bugreports.db");
            String databaseURL = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(databaseURL);
            System.out.println("Connected to local database");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to local database.");
            plugin.getLogger().severe(e.getMessage());
        }

        createTables();
    }

    private void createTables() {
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS bug_reports(rowid INTEGER, player_id TEXT, header TEXT, message TEXT, username TEXT, world TEXT, archived INTEGER DEFAULT 0, report_id INTEGER)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public void updateBugReportHeader(UUID playerId, int reportIndex) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET header = ? WHERE player_id = ? AND rowid = ?");
            String existingHeader = BugReportManager.bugReports.get(playerId).get(reportIndex);

            String[] lines = existingHeader.split("\n");
            StringBuilder newHeader = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith("hasBeenRead:")) {
                    newHeader.append("hasBeenRead: ").append(1);
                } else {
                    newHeader.append(line);
                }
                newHeader.append("\n");
            }

            statement.setString(1, newHeader.toString().trim());
            statement.setString(2, playerId.toString());
            statement.setInt(3, reportIndex + 1);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update bug report header.");
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public static void updateBugReportArchive(UUID playerId, int reportIndex, int archived) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET archived = ? WHERE player_id = ? AND report_id = ?");
            statement.setInt(1, archived);
            statement.setString(2, playerId.toString());
            statement.setInt(3, reportIndex);
            statement.executeUpdate();
            statement.close();

            String existingHeader = BugReportManager.bugReports.get(playerId).get(reportIndex - 1);

            String[] lines = existingHeader.split("\n");
            StringBuilder newHeader = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith("Archived:")) {
                    newHeader.append("Archived: ").append(archived);
                } else {
                    newHeader.append(line);
                }
                newHeader.append("\n");
            }
            List<String> reports = BugReportManager.bugReports.get(playerId);
            reports.set(reportIndex - 1, newHeader.toString().trim());
            BugReportManager.bugReports.put(playerId, reports);

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update bug report archive status.");
            plugin.getLogger().severe(e.getMessage());
        }
    }
}
