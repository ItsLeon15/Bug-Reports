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
    private String databaseFilePath;

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

                List<String> reports = bugReports.getOrDefault(playerId, new ArrayList<>());
                reports.add("Username: " + username + "\nUUID: " + playerId.toString() + "\nWorld: " + world + "\nFull Message: " + fullMessage);
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

    public void closeDatabase() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
