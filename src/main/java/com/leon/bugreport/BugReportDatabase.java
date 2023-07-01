package com.leon.bugreport;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BugReportDatabase {
    private Connection connection;
    private final String databaseFilePath;

    public BugReportDatabase(String databaseFilePath) {
        this.databaseFilePath = databaseFilePath;
        connect();
        createTables();
    }

    public void addBugReport(String username, UUID playerId, String world, String header, String fullMessage) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO bug_reports (player_id, header, message, username, world) VALUES (?, ?, ?, ?, ?)");

            statement.setString(1, playerId.toString());
            statement.setString(2, header);
            statement.setString(3, fullMessage);
            statement.setString(4, username);
            statement.setString(5, world);

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, List<String>> loadBugReports() {
        Map<UUID, List<String>> bugReports = new HashMap<>();

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM bug_reports");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                String fullMessage = resultSet.getString("message");
                String username = resultSet.getString("username");
                String world = resultSet.getString("world");
                String header = resultSet.getString("header");

                List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());
                reports.add("Username: " + username + "\nUUID: " + playerId.toString() + "\nWorld: " + world + "\nFull Message: " + fullMessage + "\nHeader: " + header);
                bugReports.put(playerId, reports);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bugReports;
    }

    private void connect() {
        try {
            File databaseFile = new File(databaseFilePath);
            String databaseURL = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(databaseURL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS bug_reports (player_id TEXT, header TEXT, message TEXT, username TEXT, world TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateBugReportHeader(UUID playerId, int reportIndex, int hasBeenRead) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE bug_reports SET header = ? WHERE player_id = ? AND rowid = ?");
            String existingHeader = BugReportManager.bugReports.get(playerId).get(reportIndex);

            String[] lines = existingHeader.split("\n");
            StringBuilder newHeader = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith("hasBeenRead:")) {
                    newHeader.append("hasBeenRead: ").append(hasBeenRead);
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
            e.printStackTrace();
        }
    }
}
