package com.leon.bugreport;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

import static com.leon.bugreport.BugReportManager.plugin;

public class BugReportPlugin extends JavaPlugin {
    private BugReportManager reportManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Failed to create data folder.");
            }
        }

        try {
            reportManager = new BugReportManager(this);
        } catch (Exception e) {
            throw new RuntimeException (e);
        }

        registerCommands();
        registerListeners();
        Metrics metrics = new Metrics(this, 18974);

        BugReportLanguage.loadLanguageTexts(plugin, "languages.yml");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("bugreport")).setExecutor(new BugReportCommand(reportManager));
        Objects.requireNonNull(getCommand("buglist")).setExecutor(new BugListCommand(reportManager));
        Objects.requireNonNull(getCommand("buglinkdiscord")).setExecutor(new LinkDiscordCommand(reportManager));
        Objects.requireNonNull(getCommand("buglistarchived")).setExecutor(new BugListArchivedCommand(reportManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(reportManager), this);
        getServer().getPluginManager().registerEvents(new BugReportCommand(reportManager), this);
        getServer().getPluginManager().registerEvents(new BugReportSettings.BugReportSettingsListener(), this);
    }
}