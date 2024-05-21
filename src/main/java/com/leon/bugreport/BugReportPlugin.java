package com.leon.bugreport;

import com.leon.bugreport.API.CacheCleanupListener;
import com.leon.bugreport.commands.BugListArchivedCommand;
import com.leon.bugreport.commands.BugListSettingsCommand;
import com.leon.bugreport.commands.BugReportCommand;
import com.leon.bugreport.commands.LinkDiscordCommand;
import com.leon.bugreport.expansions.BugPlaceholders;
import com.leon.bugreport.extensions.PlanHook;
import com.leon.bugreport.listeners.ItemDropEvent;
import com.leon.bugreport.listeners.ReportListener;
import com.leon.bugreport.listeners.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportDatabase.dataSource;
import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.gui.bugreportGUI.generateNewYML;

public class BugReportPlugin extends JavaPlugin implements Listener {
	private BugReportManager reportManager;

	@NotNull
	private static List<String> getNewReports(@NotNull List<String> reports, long lastLoginTimestamp) {
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
		return newReports;
	}

	@Override
	public void onEnable() {
		try {
			reportManager = new BugReportManager(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			if (BugReportManager.debugMode) plugin.getLogger().info("Hooking into Plan");
			PlanHook.getInstance().hookIntoPlan();
		} catch (NoClassDefFoundError planIsNotInstalled) {
			// Ignore catch
		}

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			if (BugReportManager.debugMode) plugin.getLogger().info("Hooking into PlaceholderAPI");
			new BugPlaceholders(this).register();
		}

		if (!getDataFolder().exists()) {
			if (!getDataFolder().mkdirs()) {
				plugin.getLogger().warning("Failed to create data folder.");
				logErrorMessage("Failed to create data folder.");
			}
		}

		UpdateChecker updateChecker = new UpdateChecker(this, 110732);

		if (getConfig().getBoolean("update-checker")) {
			updateChecker.getVersion(spigotVersion -> {
				String serverVersion = this.getDescription().getVersion();
				if (compareVersions(serverVersion, spigotVersion) < 0) {
					getLogger().warning("A new version of Bug Report is available: " + spigotVersion);
					if (getConfig().getBoolean("auto-update")) {
						updateChecker.checkAndUpdateIfEnabled();
					}
				}
			});
		}

		registerCommands();
		registerListeners();

		new BugReportLanguage(this);
		Metrics metrics = new Metrics(this, 18974);

		generateNewYML();
	}

	@Override
	public void onDisable() {
		if (BugReportManager.debugMode) plugin.getLogger().info("Disabling Bug Report");

		bugReports.clear();
		try {
			dataSource.close();
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to close database connection.");
			logErrorMessage("Failed to close database connection.");
		}
	}

	@EventHandler
	public void onPlayerLeave(@NotNull PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();
		BugReportDatabase.setPlayerLastLoginTimestamp(playerId);
	}

	private int compareVersions(@NotNull String version1, @NotNull String version2) {
		if (BugReportManager.debugMode) plugin.getLogger().info("Comparing versions: " + version1 + " and " + version2);
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
			if (onlinePlayer.isOp() || onlinePlayer.hasPermission("bugreport.notify")) {
				if (config.getBoolean("update-check-on-join")) {
					new UpdateChecker(this, 110732).getVersion(spigotVersion -> {
						String serverVersion = this.getDescription().getVersion();
						if (compareVersions(serverVersion, spigotVersion) < 0) {
							onlinePlayer.sendMessage(pluginColor + pluginTitle + " " + "A new version of Bug Report is available: "
									+ Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.YELLOW) + ChatColor.BOLD + spigotVersion);
						}
					});
				}

				if (!config.getBoolean("enableBugReportNotifications")) {
					return;
				}

				Player player = event.getPlayer();
				UUID playerId = player.getUniqueId();

				long lastLoginTimestamp = BugReportDatabase.getPlayerLastLoginTimestamp(playerId);

				List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
				List<String> newReports = getNewReports(reports, lastLoginTimestamp);

				if (!newReports.isEmpty()) {
					player.sendMessage(pluginColor + pluginTitle + " "
							+ Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GRAY)
							+ BugReportLanguage.getValueFromLanguageFile("newReportsMessage", "You have %numReports% new reports")
							.replace("%numReports%", String.valueOf(newReports.size())));
				} else {
					player.sendMessage(pluginColor + pluginTitle + " "
							+ Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GRAY)
							+ BugReportLanguage.getValueFromLanguageFile("noNewReportsMessage", "You have no new reports"));
				}
			}
		}
	}

	private void registerCommands() {
		if (BugReportManager.debugMode) plugin.getLogger().info("Registering commands");
		BugReportCommand bugReportCommandExecutor = new BugReportCommand(reportManager);
		UniversalTabCompleter universalTabCompleter = new UniversalTabCompleter();

		Objects.requireNonNull(getCommand("buglistarchived")).setExecutor(new BugListArchivedCommand(reportManager));
		Objects.requireNonNull(getCommand("buglistsettings")).setExecutor(new BugListSettingsCommand(reportManager));
		Objects.requireNonNull(getCommand("buglinkdiscord")).setExecutor(new LinkDiscordCommand(reportManager));

		PluginCommand bugReportCommand = Objects.requireNonNull(this.getCommand("bugreport"));
		bugReportCommand.setExecutor(bugReportCommandExecutor);

		PluginCommand bugListCommand = Objects.requireNonNull(this.getCommand("buglist"));
		bugListCommand.setTabCompleter(universalTabCompleter);
		bugListCommand.setExecutor(new BugListCommand());
	}

	private void registerListeners() {
		if (BugReportManager.debugMode) plugin.getLogger().info("Registering listeners");
		getServer().getPluginManager().registerEvents(new BugReportSettings.BugReportSettingsListener(), this);
		getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(), this);
		getServer().getPluginManager().registerEvents(new BugReportCommand(reportManager), this);
		getServer().getPluginManager().registerEvents(new ItemDropEvent(), this);
		getServer().getPluginManager().registerEvents(new ReportListener(), this);
		getServer().getPluginManager().registerEvents(this, this);
		new CacheCleanupListener();
	}
}
