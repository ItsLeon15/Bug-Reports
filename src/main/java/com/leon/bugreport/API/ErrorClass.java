package com.leon.bugreport.API;

import java.io.File;
import java.io.FileWriter;

import static com.leon.bugreport.BugReportManager.plugin;

public class ErrorClass {
	private static final File LOG_DIR = new File("plugins/BugReport/logs");
	private static final File ERROR_FILE = new File(LOG_DIR, "error.log");

	public static void logErrorMessage(String errorMessage) {
		cacheDirectoryExists();
		createErrorLog();

		try {
			appendToFile(errorMessage);
		} catch (Exception e) {
			plugin.getLogger().warning("Error 011: Failed to write to error log file");
			logErrorMessage("Error 011: Failed to write to error log file");
		}
	}

	private static void cacheDirectoryExists() {
		if (!LOG_DIR.exists() && !LOG_DIR.mkdirs()) {
			plugin.getLogger().warning("Error 012: Failed to create cache directory");
			logErrorMessage("Error 012: Failed to create cache directory");
		}
	}

	public static void createErrorLog() {
		try {
			if (!ERROR_FILE.exists() && !ERROR_FILE.createNewFile()) {
				plugin.getLogger().warning("Error 013: Failed to create error log file");
				logErrorMessage("Error 013: Failed to create error log file");
			}
		} catch (Exception ignored) {

		}
	}

	private static void appendToFile(String error) {
		try {
			String currentTime = java.time.LocalDateTime.now().toString();
			String formattedTime = currentTime.substring(0, currentTime.indexOf('T')) + ' ' + currentTime.substring(currentTime.indexOf('T') + 1, currentTime.indexOf('.'));

			FileWriter fileWriter = new FileWriter(ErrorClass.ERROR_FILE, true);
			fileWriter.write('[' + formattedTime + "] " + error + '\n');
			fileWriter.close();
		} catch (Exception e) {
			plugin.getLogger().warning("Error 014: Failed to write to error log file");
			logErrorMessage("Error 014: Failed to write to error log file");
		}
	}
}
