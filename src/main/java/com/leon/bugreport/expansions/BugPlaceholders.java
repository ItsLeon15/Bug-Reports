package com.leon.bugreport.expansions;

import com.leon.bugreport.API.ErrorClass;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import static com.leon.bugreport.BugReportDatabase.*;
import static com.leon.bugreport.BugReportManager.debugMode;

public class BugPlaceholders extends PlaceholderExpansion {
	private final Plugin plugin;

	public BugPlaceholders(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthors().toString();
	}

	@Override
	public String getIdentifier() {
		return "bugreports";
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onRequest(OfflinePlayer player, @NotNull String params) {
		if (debugMode) {
			ErrorClass.throwDebug("LinkDiscordCommand: Requested Placeholders", "debug");
		}
		switch (params) {
			case "totalBugReports" -> {
				return String.valueOf(loadBugReportCount());
			}
			case "totalArchivedBugReports" -> {
				return String.valueOf(loadArchivedBugReportCount());
			}
			case "totalNonArchivedBugReports" -> {
				return String.valueOf(loadNonArchivedBugReportCount());
			}
			case "totalDeletedBugReports" -> {
				return String.valueOf(loadDeletedBugReportCount());
			}
			default -> {
				return null;
			}
		}
	}
}
