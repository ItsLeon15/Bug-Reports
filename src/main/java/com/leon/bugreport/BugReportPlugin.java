package com.leon.bugreport;

import com.leon.bugreport.API.CacheCleanupListener;
import com.leon.bugreport.commands.BugListArchivedCommand;
import com.leon.bugreport.commands.BugListSettingsCommand;
import com.leon.bugreport.commands.BugReportCommand;
import com.leon.bugreport.commands.LinkDiscordCommand;
import com.leon.bugreport.listeners.ReportListener;
import com.leon.bugreport.listeners.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportManager.*;

public class BugReportPlugin extends JavaPlugin implements Listener {
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
            throw new RuntimeException(e);
        }

        registerCommands();
        registerListeners();
        Metrics metrics = new Metrics(this, 18974);

        BugReportLanguage.loadLanguageTexts(plugin, "languages.yml");
    }

    @Override
    public void onDisable() {
        bugReports.clear();
        try {
            BugReportDatabase.dataSource.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to close database connection.");
        }
    }

    @EventHandler
    public void onPlayerLeave(@NotNull PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        BugReportDatabase.setPlayerLastLoginTimestamp(playerId);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOp()) {
                Player player = event.getPlayer();
                new UpdateChecker(this, 110732).getVersion(spigotVersion -> {
                    int intServerVersion = Integer.parseInt(this.getDescription().getVersion().split("v")[1]);
                    int intSpigotVersion = Integer.parseInt(spigotVersion.split("v")[1]);

                    if (intServerVersion < intSpigotVersion) {
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GRAY + "A new version of Bug Report is available: " + ChatColor.YELLOW + ChatColor.BOLD + spigotVersion);
                    }
                });
            }

            if (onlinePlayer.hasPermission("bugreport.notify")) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();

                long lastLoginTimestamp = BugReportDatabase.getPlayerLastLoginTimestamp(playerId);

                List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
                List<String> newReports = new ArrayList<>();
                for (String report : reports) {
                    String[] lines = report.split("\n");
                    long reportTimestampLong = 0;
                    for (String line : lines) {
                        if (line.startsWith("Timestamp:")) {
                            reportTimestampLong = Long.parseLong(line.substring(10).trim());
                        }
                    }
                    if (reportTimestampLong > lastLoginTimestamp) {
                        newReports.add(report);
                    }
                }

                if (!newReports.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + pluginTitle + " " + ChatColor.GRAY + DefaultLanguageSelector.getTextElseDefault(language, "newReportsMessage")
                            .replace("%numReports%", String.valueOf(newReports.size()))
                    );
                } else {
                    player.sendMessage(ChatColor.YELLOW + pluginTitle + " " + ChatColor.GRAY + DefaultLanguageSelector.getTextElseDefault(language, "noNewReportsMessage"));
                }
            }
        }
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("buglistarchived")).setExecutor(new BugListArchivedCommand(reportManager));
        Objects.requireNonNull(getCommand("buglistsettings")).setExecutor(new BugListSettingsCommand(reportManager));
        Objects.requireNonNull(getCommand("buglinkdiscord")).setExecutor(new LinkDiscordCommand(reportManager));
        Objects.requireNonNull(getCommand("bugreport")).setExecutor(new BugReportCommand(reportManager));
        Objects.requireNonNull(getCommand("buglist")).setExecutor(new BugListCommand(reportManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BugReportSettings.BugReportSettingsListener(), this);
        getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(), this);
        getServer().getPluginManager().registerEvents(new BugReportCommand(reportManager), this);
        getServer().getPluginManager().registerEvents(new ReportListener(), this);
        getServer().getPluginManager().registerEvents(this, this);
        new CacheCleanupListener();
    }
}