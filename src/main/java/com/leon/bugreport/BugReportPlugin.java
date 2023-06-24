package com.leon.bugreport;

import org.bukkit.plugin.java.JavaPlugin;

public class BugReportPlugin extends JavaPlugin {

    private BugReportManager reportManager;
    private String dbFilePath = "bugreports.db";

    @Override
    public void onEnable() {
        reportManager = new BugReportManager(this, dbFilePath);
        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        // No need to close the H2 database connection
    }

    private void registerCommands() {
        getCommand("bugreport").setExecutor(new BugReportCommand(reportManager));
        getCommand("buglist").setExecutor(new BugListCommand(reportManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(reportManager, this), this);
    }
}
