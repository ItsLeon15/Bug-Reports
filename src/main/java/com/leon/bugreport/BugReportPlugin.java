package com.leon.bugreport;

import com.leon.bugreport.API.CacheCleanupListener;
import com.leon.bugreport.commands.*;
import com.leon.bugreport.expansions.BugPlaceholders;
import com.leon.bugreport.extensions.PlanHook;
import com.leon.bugreport.listeners.ItemDropEvent;
import com.leon.bugreport.listeners.ReportListener;
import com.leon.bugreport.listeners.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
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
	private static BugReportPlugin instance;
	private final UpdateChecker updateChecker = new UpdateChecker(this, 110732);
	private final Set<UUID> notifiedPlayers = new HashSet<>();
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

	public static BugReportPlugin getPlugin() {
		return instance;
	}

	@Override
	public void onEnable() {
		reportManager = new BugReportManager(this);

		try {
			PlanHook.getInstance().hookIntoPlan();
		} catch (NoClassDefFoundError planIsNotInstalled) {
			// Ignore catch
		}

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new BugPlaceholders(this).register();
		}

		if (!getDataFolder().exists()) {
			if (!getDataFolder().mkdirs()) {
				plugin.getLogger().warning("Failed to create data folder.");
				logErrorMessage("Failed to create data folder.");
			}
		}

		if (getConfig().getBoolean("update-checker")) {
			updateChecker.getVersion(spigotVersion -> {
				String serverVersion = this.getDescription().getVersion();
				if (compareVersions(serverVersion, spigotVersion) < 0) {
					plugin.getLogger().info("A new version of Bug Report is available: " + spigotVersion);
				}
			});
		}

		registerCommands();
		registerListeners();

		instance = this;

		new BugReportLanguage(this);

		if (getConfig().getBoolean("metrics")) {
			new Metrics(this, 18974);
		}

		generateNewYML();
	}

	@Override
	public void onDisable() {
		bugReports.clear();

		try {
			dataSource.close();
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to close database connection.");
			logErrorMessage("Failed to close database connection.");
		}

		this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
		this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
	}

	@EventHandler
	public void onPlayerLeave(@NotNull PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();
		BugReportDatabase.setPlayerLastLoginTimestamp(playerId);
	}

	private int compareVersions(@NotNull String version1, @NotNull String version2) {
		if (debugMode) {
			plugin.getLogger().info("Comparing versions: " + version1 + " and " + version2);
		}
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
				UUID playerId = onlinePlayer.getUniqueId();

				if (!config.getBoolean("enableBugReportNotifications")) {
					continue;
				}

				if (notifiedPlayers.contains(playerId)) {
					return;
				}

				long lastLoginTimestamp = BugReportDatabase.getPlayerLastLoginTimestamp(playerId);

				List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
				List<String> newReports = getNewReports(reports, lastLoginTimestamp);

				StringBuilder message = new StringBuilder();
				if (!newReports.isEmpty()) {
					message.append(pluginColor)
							.append(pluginTitle).append(" ")
							.append(Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GRAY))
							.append(BugReportLanguage.getValueFromLanguageFile("newReportsMessage", "You have %numReports% new reports")
									.replace("%numReports%", String.valueOf(newReports.size())))
							.append("\n");
				} else {
					message.append(pluginColor)
							.append(pluginTitle).append(" ")
							.append(Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GRAY))
							.append(BugReportLanguage.getValueFromLanguageFile("noNewReportsMessage", "You have no new reports"))
							.append("\n");
				}

				if (getConfig().getBoolean("update-checker-join")) {
					updateChecker.getVersion(spigotVersion -> {
						String serverVersion = this.getDescription().getVersion();
						if (compareVersions(serverVersion, spigotVersion) < 0) {
							message.append(pluginColor).append(pluginTitle).append(" ")
									.append(Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GRAY))
									.append("A new version of Bug Report is available:")
									.append(ChatColor.YELLOW).append(" v").append(spigotVersion);
							onlinePlayer.sendMessage(message.toString());
						}
					});
				} else {
					onlinePlayer.sendMessage(message.toString());
				}

				notifiedPlayers.add(playerId);
			}
		}
	}

	private void registerCommands() {
		if (BugReportManager.debugMode) plugin.getLogger().info("Registering commands");

		this.saveDefaultConfig();
		FileConfiguration config = this.getConfig();

		BugReportCommand bugReportCommandExecutor = new BugReportCommand(reportManager);
		UniversalTabCompleter universalTabCompleter = new UniversalTabCompleter(reportManager, config);

		Objects.requireNonNull(getCommand("buglistarchived")).setExecutor(new BugListArchivedCommand());
		Objects.requireNonNull(getCommand("buglistsettings")).setExecutor(new BugListSettingsCommand(reportManager));
		Objects.requireNonNull(getCommand("buglinkdiscord")).setExecutor(new LinkDiscordCommand(reportManager));

		PluginCommand bugReportCommand = Objects.requireNonNull(this.getCommand("bugreport"));
		bugReportCommand.setTabCompleter(universalTabCompleter);
		bugReportCommand.setExecutor(bugReportCommandExecutor);

		PluginCommand bugListCommand = Objects.requireNonNull(this.getCommand("buglist"));
		bugListCommand.setTabCompleter(universalTabCompleter);
		bugListCommand.setExecutor(new BugListCommand());
	}

	private void registerListeners() {
		getServer().getPluginManager().registerEvents(new BugReportSettings.BugReportSettingsListener(), this);
		getServer().getPluginManager().registerEvents(new BugReportManager.BugReportListener(), this);
		getServer().getPluginManager().registerEvents(new BugReportCommand(reportManager), this);
		getServer().getPluginManager().registerEvents(new ItemDropEvent(), this);
		getServer().getPluginManager().registerEvents(new ReportListener(), this);
		getServer().getPluginManager().registerEvents(this, this);

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new com.leon.bugreport.listeners.PluginMessageListener());

		new CacheCleanupListener();
	}
}
