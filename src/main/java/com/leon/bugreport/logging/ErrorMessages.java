package com.leon.bugreport.logging;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ErrorMessages {
	private static final Map<Integer, String> errorMessages = new HashMap<>();

	static {
		errorMessages.put(1, "discordEmbedFields key is not present in the config. Sending an empty embed");
		errorMessages.put(2, "discordEmbedFields is empty in the config. Bug report not sent to Discord");
		errorMessages.put(3, "Failed to load cache");
		errorMessages.put(4, "Failed to save cache");
		errorMessages.put(5, "Failed to create cache directory");
		errorMessages.put(6, "Failed to get player head for %playerName%");
		errorMessages.put(7, "Failed to set custom player head texture");
		errorMessages.put(8, "Failed to get cached player head for %playerName%");
		errorMessages.put(9, "Base64 string is empty. Cannot set custom player head texture");
		errorMessages.put(10, "Failed to set custom player head texture");
		errorMessages.put(11, "Failed to write to error log file");
		errorMessages.put(12, "Failed to create cache directory");
		errorMessages.put(13, "Failed to create error log file");
		errorMessages.put(14, "Failed to write to error log file");
		errorMessages.put(15, "Invalid database type. Please use 'local' or 'mysql'");
		errorMessages.put(16, "Invalid language code %languageCode%. Defaulting to 'en_US'");
		errorMessages.put(17, "No language files found in the 'languages' folder");
		errorMessages.put(18, "Language file '%languageCode%.yml' is empty. Creating new file");
		errorMessages.put(19, "Language file '%languageCode%.yml' not found in resources");
		errorMessages.put(20, "Missing reportCategories in config.yml");
		errorMessages.put(21, "Missing '%key%' in reportCategories in config.yml");
		errorMessages.put(22, "Error saving config.yml");
		errorMessages.put(23, "Something went wrong while loading the report categories");
		errorMessages.put(24, "Missing webhookURL in config.yml");
		errorMessages.put(25, "Error sending notification to Discord");
		errorMessages.put(26, "Failed to create data folder");
		errorMessages.put(27, "Failed to close database connection");
		errorMessages.put(28, "Failed to create custom player head");
		errorMessages.put(29, "enablePluginReportCategoriesGUI and enablePluginReportCategoriesTabComplete are both true! Either one of them has to be true or both false. Using the default UI");
		errorMessages.put(30, "Failed to find and remove book for player %playerName%");
		errorMessages.put(31, "Failed to remove book for player %playerName%");
		errorMessages.put(32, "Failed to submit bug report");
		errorMessages.put(33, "Unable to check for updates");
		errorMessages.put(34, "Cache is invalid");
		errorMessages.put(35, "Failed to add missing columns");
		errorMessages.put(36, "Failed to set player last login timestamp");
		errorMessages.put(37, "Failed to get player last login timestamp");
		errorMessages.put(38, "Failed to get bug report location");
		errorMessages.put(39, "Failed to add missing columns");
		errorMessages.put(40, "Failed to load bug reports");
		errorMessages.put(41, "Your database is corrupted. Please delete the database file and restart the server. File path: plugins/BugReport/bugreports.db");
		errorMessages.put(42, "Error getting UUID from API");
		errorMessages.put(43, "Something went wrong with the languages folder. Please remove the folder and restart the server");
		errorMessages.put(44, "The layout of the customGUI.yml file is incorrect. Falling back to the default layout");
		errorMessages.put(45, "Error sending additional pings to Discord");
		errorMessages.put(46, "Failed to export all bug reports");
		errorMessages.put(47, "Failed to submit bug report to Plan");
	}

	public static @NotNull String getErrorMessage(int errorNumber) {
		return errorMessages.getOrDefault(errorNumber, "Unknown error");
	}

	public static @NotNull String getErrorMessageWithAdditionalMessage(int errorNumber, String additionalMessage) {
		return errorMessages.getOrDefault(errorNumber, "Unknown error") + ": " + additionalMessage;
	}
}
