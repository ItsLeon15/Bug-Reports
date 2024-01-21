package com.leon.bugreport;

import com.leon.bugreport.API.CacheCleanupListener;
import com.leon.bugreport.commands.BugListArchivedCommand;
import com.leon.bugreport.commands.BugListSettingsCommand;
import com.leon.bugreport.commands.BugReportCommand;
import com.leon.bugreport.commands.LinkDiscordCommand;
import com.leon.bugreport.expansions.BugPlaceholders;
import com.leon.bugreport.extensions.PlanHook;
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

import static com.leon.bugreport.BugReportDatabase.dataSource;
import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportManager.*;

public class BugReportPlugin extends JavaPlugin implements Listener {
    private BugReportManager reportManager;

    @Override
    public void onEnable() {
        try {
            PlanHook.getInstance().hookIntoPlan();
        } catch (NoClassDefFoundError planIsNotInstalled) {
            // Ignore catch
        }

        try {
            reportManager = new BugReportManager(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new BugPlaceholders(this).register();
        }

        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Failed to create data folder.");
            }
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
            dataSource.close();
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

    private int compareVersions(@NotNull String version1, @NotNull String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int minLength = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < minLength; i++) {
            int part1 = Integer.parseInt(parts1[i]);
            int part2 = Integer.parseInt(parts2[i]);

            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }

        if (parts1.length < parts2.length) {
            return -1;
        } else if (parts1.length > parts2.length) {
            return 1;
        }

        return 0;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOp()) {
                new UpdateChecker(this, 110732).getVersion(spigotVersion -> {
                    String serverVersion = this.getDescription().getVersion();
                    if (compareVersions(serverVersion, spigotVersion) < 0) {
                        onlinePlayer.sendMessage(pluginColor + pluginTitle + " " + "A new version of Bug Report is available: " + ChatColor.YELLOW + ChatColor.BOLD + spigotVersion);
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