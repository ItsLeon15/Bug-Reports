package com.leon.bugreport.API;

import com.leon.bugreport.logging.ErrorMessages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportLanguage.getValueFromLanguageFile;
import static com.leon.bugreport.BugReportManager.*;

public class DataBackup {
	public static void exportAllBugReports(@NotNull Player player) {
		playButtonClickSound(player);

		if (debugMode) {
			plugin.getLogger().info("Export all bug reports clicked by " + player.getName());
		}

		player.closeInventory();
		player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + getValueFromLanguageFile("exportAllBugReports", "Exporting all bug reports..."));

		File reportFile = new File(plugin.getDataFolder(), "all_exported_bug_reports.csv");

		Set<String> headers = new LinkedHashSet<>();
		List<Map<String, String>> parsedReports = new ArrayList<>();

		for (Map.Entry<UUID, List<String>> entry : bugReports.entrySet()) {
			List<String> logDetailsList = entry.getValue();

			for (String logDetails : logDetailsList) {
				Map<String, String> parsedReport = parseLogEntry(logDetails);
				headers.addAll(parsedReport.keySet());
				parsedReports.add(parsedReport);
			}
		}

		try (FileWriter csvWriter = new FileWriter(reportFile)) {
			csvWriter.append(String.join(",", headers)).append("\n");

			for (Map<String, String> report : parsedReports) {
				List<String> row = new ArrayList<>();
				for (String header : headers) {
					String value = report.getOrDefault(header, "");
					if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
						value = "\"" + value.replace("\"", "\"\"") + "\"";
					}
					row.add(value);
				}
				csvWriter.append(String.join(",", row)).append("\n");
			}

		} catch (IOException e) {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + getValueFromLanguageFile("exportAllBugReportsFailed", "Failed to export all bug reports"));

			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(46, e.getMessage());
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}

		player.sendMessage(returnStartingMessage(ChatColor.GREEN) + getValueFromLanguageFile("exportAllBugReportsSuccess", "Exported all bug reports"));
	}

	private static @NotNull Map<String, String> parseLogEntry(String log) {
		Map<String, String> parsedData = new LinkedHashMap<>();

		Pattern pattern = Pattern.compile("(?m)(^\\w.+?):\\s(.+)");
		Matcher matcher = pattern.matcher(log);

		while (matcher.find()) {
			String key = matcher.group(1).trim();
			String value = matcher.group(2).trim();
			parsedData.put(key, value);
		}

		return parsedData;
	}
}
