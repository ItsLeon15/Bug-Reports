package com.leon.bugreport;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import static com.leon.bugreport.BugReportManager.plugin;

public class BugReportPlugin extends JavaPlugin {
    private BugReportManager reportManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        reportManager = new BugReportManager(this);
        registerCommands();
        registerListeners();
        Metrics metrics = new Metrics(this, 18974);

        BugReportLanguage.loadLanguageTexts(plugin, "languages.yml");
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