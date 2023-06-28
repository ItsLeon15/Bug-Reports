package com.leon.bugreport;

import org.bukkit.plugin.java.JavaPlugin;

public class BugReportPlugin extends JavaPlugin {

    private BugReportManager reportManager;

    @Override
    public void onEnable() {
        String dbFilePath = "plugins/BugReport/bugreports.db";
        reportManager = new BugReportManager(this, dbFilePath);
        registerCommands();
        registerListeners();

    }

    @Override
    public void onDisable() {
        reportManager.saveConfig();
    }

    private void registerCommands() {
        getCommand("bugreport").setExecutor(new BugReportCommand(reportManager));
        getCommand("buglist").setExecutor(new BugListCommand(reportManager));
        getCommand("buglinkdiscord").setExecutor(new LinkDiscordCommand(reportManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(reportManager), this);
    }
}
