package com.leon.bugreport;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

public class BugReportPlugin extends JavaPlugin {

    private BugReportManager reportManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        String dbFilePath = "plugins/BugReport/bugreports.db";
        reportManager = new BugReportManager(this, dbFilePath);
        registerCommands();
        registerListeners();
        metrics = new Metrics(this, 18974);
    }

    private void registerCommands() {
        getCommand("bugreport").setExecutor(new BugReportCommand(reportManager));
        getCommand("buglist").setExecutor(new BugListCommand(reportManager));
        getCommand("buglinkdiscord").setExecutor(new LinkDiscordCommand(reportManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(reportManager), this);
        getServer().getPluginManager().registerEvents(new BugReportCommand(reportManager), this);
        getServer().getPluginManager().registerEvents(new BugReportSettings.BugReportSettingsListener(), this);
    }
}