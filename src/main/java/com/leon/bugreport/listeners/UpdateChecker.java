package com.leon.bugreport.listeners;

import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {
	private final JavaPlugin plugin;
	private final int resourceId;

	public UpdateChecker(JavaPlugin plugin, int resourceId) {
		this.plugin = plugin;
		this.resourceId = resourceId;
	}

	public void getVersion(final Consumer<String> consumer) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openStream()) {
					try (Scanner scanner = new Scanner(is)) {
						if (scanner.hasNext()) {
							consumer.accept(scanner.next());
						}
					}
				} catch (IOException e) {
					plugin.getLogger().warning("Unable to check for updates: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	public void downloadUpdate(String version) {
		new BukkitRunnable() {
			@Override
			public void run() {
				String downloadUrl = "https://api.spiget.org/v2/resources/" + resourceId + "/download";
				try (InputStream in = new URL(downloadUrl).openStream()) {
					File pluginFile = plugin.getDataFolder().getParentFile();
					File newFile = new File(pluginFile, "BugReports-" + version + ".jar");
					FileUtils.copyInputStreamToFile(in, newFile);
					plugin.getLogger().info("Update downloaded successfully.");
				} catch (IOException e) {
					plugin.getLogger().warning("Failed to download update: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	public void checkAndUpdateIfEnabled() {
		if (plugin.getConfig().getBoolean("auto-update")) {
			getVersion(version -> {
				String currentVersion = plugin.getDescription().getVersion();
				if (!version.equals(currentVersion)) {
					plugin.getLogger().info("New version available: " + version);
					downloadUpdate(version);
				} else {
					plugin.getLogger().info("Plugin is up to date.");
				}
			});
		} else {
			plugin.getLogger().info("Auto-update is disabled.");
		}
	}
}
